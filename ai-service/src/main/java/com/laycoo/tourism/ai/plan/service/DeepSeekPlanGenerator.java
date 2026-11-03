package com.laycoo.tourism.ai.plan.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laycoo.tourism.ai.config.AiProperties;
import com.laycoo.tourism.ai.plan.dto.GeneratedPlan;
import com.laycoo.tourism.ai.plan.dto.PlanContextItem;
import com.laycoo.tourism.ai.plan.entity.TravelPlan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DeepSeek / OpenAI 兼容接口的行程生成器。
 * Key 只从 AiProperties（环境变量 AI_API_KEY）读取，不落库、不写日志。
 */
@Slf4j
@Component
public class DeepSeekPlanGenerator implements PlanGenerator {

    private static final String SYSTEM_PROMPT = """
            你是「岚途 LayCoo」的本地旅行规划师，擅长把一座城市的吃喝玩乐与人文历史
            安排成可执行的多日行程。

            要求：
            1. 只输出 JSON，不要输出 markdown 代码块，不要输出任何解释。
            2. 用户会给出开始日期、结束日期和天数 N，必须输出 N 个 day，日期连续。
            3. 每天安排 3-6 个节点，时间必须递增，符合真实移动时间。
            4. 行程要考虑老人、孩子、体力等补充说明。
            5. 优先使用【本地内容】中的地点；如引用，填写 sourceContentId。
            6. 不同天之间尽量不重复地点；内容不足时再复用并调整时段。
            7. 不要编造门票、电话、营业时间等精确信息；不确定时写“以现场为准”。
            8. 不输出违规、低俗、广告导流内容。
            9. 中文输出，语气克制、具体、有画面感。
            10. 必须规划出发地到目的地的去程与返程交通。不要编造具体车次号、航班号或精确票价；
                给出交通方式、建议出发/到达时段、班次类型和注意事项，并注明以 12306 / 航司实时信息为准。

            JSON 顶层字段：
            title, summary, city, startDate, endDate, dayCount, preferences,
            intercityTransport, days, budget, rainPlan, packingTips, warnings

            intercityTransport 字段：
            outbound, return, note

            outbound / return 字段：
            date, mode, title, duration, departureWindow, arrivalWindow, options, tip

            mode 只能是：TRAIN / FLIGHT / MIXED。
            options 每项：type, label, note。最多 4 项，至少 1 项。
            outbound.date 必须等于 startDate，return.date 必须等于 endDate。
            note 写“具体车次、航班、票价与余票以 12306 或航空公司实时信息为准”。

            days[i] 字段：
            day, date, theme, summary, timeline, food, transport

            timeline[j] 字段：
            time, durationMinutes, type, title, area, address, description, tips, sourceContentId

            type 只能是：SPOT / FOOD / EXPERIENCE / SHOP / REST / OTHER。
            time 使用 HH:mm，24 小时制。
            """;

    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public DeepSeekPlanGenerator(AiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(Math.max(properties.getTimeoutSeconds(), 30) * 1000);
        this.restClient = RestClient.builder()
                .baseUrl(trimTrailingSlash(properties.getBaseUrl()))
                .requestFactory(factory)
                .build();
    }

    @Override
    public GeneratedPlan generate(TravelPlan plan, List<PlanContextItem> context) {
        String userPrompt = buildUserPrompt(plan, context);
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", properties.getModel());
        request.put("messages", List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", userPrompt)));
        request.put("temperature", 0.7);
        request.put("max_tokens", maxTokens(plan.getDayCount()));
        request.put("response_format", Map.of("type", "json_object"));

        Exception lastError = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> response = restClient.post()
                        .uri("/chat/completions")
                        .header("Authorization", "Bearer " + properties.getApiKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .body(Map.class);
                return parse(response, plan, userPrompt);
            } catch (Exception e) {
                lastError = e;
                log.warn("DeepSeek 行程生成失败（第 {} 次）: {}", attempt, e.getMessage());
            }
        }
        throw new PlanGenerationException("DeepSeek 调用失败: "
                + (lastError == null ? "未知错误" : lastError.getMessage()), lastError);
    }

    @SuppressWarnings("unchecked")
    private GeneratedPlan parse(Map<String, Object> response, TravelPlan plan, String userPrompt) throws Exception {
        if (response == null) {
            throw new PlanGenerationException("DeepSeek 返回为空");
        }
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new PlanGenerationException("DeepSeek 返回缺少 choices");
        }
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        String content = message == null ? null : String.valueOf(message.get("content"));
        if (!StringUtils.hasText(content)) {
            throw new PlanGenerationException("DeepSeek 返回内容为空");
        }
        JsonNode planJson = objectMapper.readTree(stripCodeFence(content));
        long tokens = 0;
        Object usage = response.get("usage");
        if (usage instanceof Map<?, ?> usageMap && usageMap.get("total_tokens") instanceof Number number) {
            tokens = number.longValue();
        }
        return new GeneratedPlan(planJson, properties.getModel(), tokens, userPrompt);
    }

    private String buildUserPrompt(TravelPlan plan, List<PlanContextItem> context) {
        String contextJson;
        try {
            contextJson = objectMapper.writeValueAsString(context == null ? List.of() : context);
        } catch (Exception e) {
            contextJson = "[]";
        }
        String preferences = plan.getPreferences() == null ? "[]" : plan.getPreferences();
        return "出发地：" + plan.getOriginCityName()
                + "\n目的地：" + plan.getCityName()
                + "\n出发日期：" + plan.getStartDate()
                + "\n返程日期：" + plan.getEndDate()
                + "\n交通偏好：" + transportLabel(plan.getTransportPreference())
                + "\n天数：" + plan.getDayCount() + " 天"
                + "\n偏好：" + preferences
                + "\n补充说明：" + (plan.getExtraNote() == null ? "无" : plan.getExtraNote())
                + "\n\n【本地内容】\n" + contextJson
                + "\n\n请严格按要求的 JSON 结构输出完整行程，并包含去程、返程交通方案。";
    }

    private String transportLabel(String preference) {
        if (preference == null) {
            return "不限";
        }
        return switch (preference) {
            case "TRAIN" -> "优先高铁";
            case "FLIGHT" -> "优先航班";
            case "BOTH" -> "高铁与航班都给出";
            default -> "不限，按时间与舒适度选择";
        };
    }

    private int maxTokens(int dayCount) {
        return Math.min(2000 + Math.max(dayCount, 1) * 1800, 8192);
    }

    private String stripCodeFence(String content) {
        String text = content.trim();
        if (text.startsWith("```")) {
            int firstLine = text.indexOf('\n');
            if (firstLine > 0) {
                text = text.substring(firstLine + 1);
            }
            if (text.endsWith("```")) {
                text = text.substring(0, text.length() - 3);
            }
        }
        return text.trim();
    }

    private static String trimTrailingSlash(String url) {
        return url != null && url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
