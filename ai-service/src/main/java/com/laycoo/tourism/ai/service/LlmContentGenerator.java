package com.laycoo.tourism.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laycoo.tourism.ai.config.AiProperties;
import com.laycoo.tourism.ai.dto.AiContent;
import com.laycoo.tourism.ai.dto.GeneratedResult;
import com.laycoo.tourism.ai.entity.AiTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容协议的大模型客户端（DeepSeek / 通义 / 豆包均可通过 base-url + model 切换）。
 * 要求模型输出 JSON，服务端做结构化解析与字段校验。
 */
@Slf4j
@Component
public class LlmContentGenerator implements ContentGenerator {

    private static final String SYSTEM_PROMPT = """
            你是「本地文旅智能推荐平台」的资深内容编辑，擅长写真实、克制、有细节的中文文旅内容。
            请严格按照用户要求，只输出一个 JSON 对象，不要输出 markdown 代码块标记，不要输出任何解释。

            JSON 结构必须为：
            {"contents":[{"title":"标题","summary":"一句话摘要","categoryCode":"分类编码","area":"区域",
            "address":"详细地址","body":"markdown 正文","highlights":["亮点1","亮点2","亮点3"],
            "tips":"实用贴士","openingHours":"营业时间","priceRange":"参考价格",
            "coverDesc":"封面画面描述","tags":["标签1","标签2"]}]}

            写作要求：
            1. 标题 8-30 字，具体、有画面感，不要使用「必去」「天花板」等夸张营销词。
            2. 摘要 40-90 字，说清是什么、在哪里、为什么值得去。
            3. 正文 300-600 字，必须包含三个 markdown 二级标题：## 为什么值得去、## 特色体验、## 实用信息。
            4. 不要编造门票、电话等精确事实；不确定的信息用「以现场为准」表述。
            5. 不出现违规、低俗、广告导流内容，不出现「待补充」「TODO」等占位符。
            """;

    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public LlmContentGenerator(AiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(Math.max(properties.getTimeoutSeconds(), 10) * 1000);
        this.restClient = RestClient.builder()
                .baseUrl(trimTrailingSlash(properties.getBaseUrl()))
                .requestFactory(factory)
                .build();
    }

    @Override
    public GeneratedResult generate(AiTask task) {
        String userPrompt = buildUserPrompt(task);
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", properties.getModel());
        request.put("messages", List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", userPrompt)));
        request.put("temperature", properties.getTemperature());
        request.put("max_tokens", 3000);
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
                GeneratedResult result = parse(response, task, userPrompt);
                if (!result.getContents().isEmpty()) {
                    return result;
                }
                lastError = new IllegalStateException("模型未返回可用内容");
            } catch (Exception e) {
                lastError = e;
                log.warn("大模型调用失败（第 {} 次）: {}", attempt, e.getMessage());
            }
        }
        throw new IllegalStateException("大模型调用失败: "
                + (lastError == null ? "未知错误" : lastError.getMessage()), lastError);
    }

    @SuppressWarnings("unchecked")
    private GeneratedResult parse(Map<String, Object> response, AiTask task, String userPrompt) throws Exception {
        if (response == null) {
            throw new IllegalStateException("大模型返回为空");
        }
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new IllegalStateException("大模型返回缺少 choices");
        }
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        String content = message == null ? null : String.valueOf(message.get("content"));
        if (!StringUtils.hasText(content)) {
            throw new IllegalStateException("大模型返回内容为空");
        }
        JsonNode root = objectMapper.readTree(stripCodeFence(content));
        JsonNode array = root.isArray() ? root : root.path("contents");
        if (!array.isArray() || array.isEmpty()) {
            throw new IllegalStateException("大模型返回的 JSON 中没有 contents 数组");
        }
        List<AiContent> contents = new ArrayList<>();
        for (JsonNode node : array) {
            AiContent item = new AiContent();
            item.setTitle(text(node, "title"));
            item.setSummary(text(node, "summary"));
            item.setCategoryCode(StringUtils.hasText(text(node, "categoryCode"))
                    ? text(node, "categoryCode") : task.getCategoryCode());
            item.setArea(StringUtils.hasText(text(node, "area")) ? text(node, "area") : task.getArea());
            item.setAddress(text(node, "address"));
            item.setBody(text(node, "body"));
            item.setHighlights(textArray(node, "highlights"));
            item.setTips(text(node, "tips"));
            item.setOpeningHours(text(node, "openingHours"));
            item.setPriceRange(text(node, "priceRange"));
            item.setCoverDesc(text(node, "coverDesc"));
            item.setTags(textArray(node, "tags"));
            item.setAiTaskId(task.getId());
            contents.add(item);
        }
        long tokens = 0;
        Object usage = response.get("usage");
        if (usage instanceof Map<?, ?> usageMap && usageMap.get("total_tokens") instanceof Number number) {
            tokens = number.longValue();
        }
        return new GeneratedResult(contents, tokens, properties.getModel(), userPrompt);
    }

    private String buildUserPrompt(AiTask task) {
        return "分类编码：" + task.getCategoryCode()
                + "\n区域：" + (StringUtils.hasText(task.getArea()) ? task.getArea() : "杭州")
                + "\n数量：" + task.getTargetCount()
                + "\n关键词：" + (StringUtils.hasText(task.getKeywords()) ? task.getKeywords() : "无")
                + "\n请生成符合要求的中文文旅内容。";
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? "" : value.asText("").trim();
    }

    private List<String> textArray(JsonNode node, String field) {
        List<String> values = new ArrayList<>();
        JsonNode value = node.get(field);
        if (value != null && value.isArray()) {
            for (JsonNode item : value) {
                if (StringUtils.hasText(item.asText())) {
                    values.add(item.asText().trim());
                }
            }
        }
        return values;
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
