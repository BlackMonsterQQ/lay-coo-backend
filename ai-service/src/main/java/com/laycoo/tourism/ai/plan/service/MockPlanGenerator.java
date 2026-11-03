package com.laycoo.tourism.ai.plan.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.laycoo.tourism.ai.plan.dto.GeneratedPlan;
import com.laycoo.tourism.ai.plan.dto.PlanContextItem;
import com.laycoo.tourism.ai.plan.entity.TravelPlan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 离线演示生成器：没有 DeepSeek Key 时使用，输出结构与真实模型一致。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MockPlanGenerator implements PlanGenerator {

    private static final String[] TIMES = {"09:00", "11:30", "14:30", "17:30"};
    private static final String[] GENERIC_TITLES = {
            "城市地标漫步", "本地风味午餐", "老街与手作", "河畔夜景"
    };

    private final ObjectMapper objectMapper;

    @Override
    public GeneratedPlan generate(TravelPlan plan, List<PlanContextItem> context) {
        List<PlanContextItem> items = context == null ? List.of() : context;
        ObjectNode root = objectMapper.createObjectNode();
        String theme = items.isEmpty() ? "城市漫游" : items.get(0).getCategoryName() + "与城市日常";
        root.put("title", plan.getCityName() + " " + plan.getDayCount() + " 日：" + theme);
        root.put("summary", "这是一份为「" + plan.getCityName() + "」定制的 " + plan.getDayCount()
                + " 天行程，围绕你选择的偏好安排每日节奏，适合慢慢走、慢慢看，具体开放时间以现场为准。");
        root.put("city", plan.getCityName());
        root.put("startDate", plan.getStartDate().toString());
        root.put("endDate", plan.getEndDate().toString());
        root.put("dayCount", plan.getDayCount());
        ArrayNode prefs = root.putArray("preferences");
        parsePreferences(plan).forEach(prefs::add);

        ObjectNode intercity = root.putObject("intercityTransport");
        intercity.set("outbound", transportLeg(plan, plan.getOriginCityName(), plan.getCityName(),
                plan.getStartDate().toString(), true));
        intercity.set("return", transportLeg(plan, plan.getCityName(), plan.getOriginCityName(),
                plan.getEndDate().toString(), false));
        intercity.put("note", "SELF_DRIVE".equals(plan.getTransportPreference())
                ? "以上为 AI 参考自驾路线，实际里程、路况、过路费、油费与停车请以导航和现场信息为准。"
                : "以上为 AI 参考交通方案，具体车次、航班、票价与余票请以 12306 或航空公司实时信息为准。");
        if ("SELF_DRIVE".equals(plan.getTransportPreference())) {
            root.set("driving", drivingInfo(plan));
        }

        ArrayNode days = root.putArray("days");
        List<PlanContextItem> foods = items.stream()
                .filter(item -> "FOOD".equals(item.getCategoryCode()))
                .toList();
        List<PlanContextItem> spots = items.stream()
                .filter(item -> List.of("SPOT", "HERITAGE", "FIGURE", "FOLK", "PLAY")
                        .contains(item.getCategoryCode()))
                .toList();
        List<PlanContextItem> experiences = items.stream()
                .filter(item -> List.of("INTANGIBLE", "SHOP", "STAY").contains(item.getCategoryCode()))
                .toList();
        for (int dayIndex = 0; dayIndex < plan.getDayCount(); dayIndex++) {
            LocalDate date = plan.getStartDate().plusDays(dayIndex);
            ObjectNode day = days.addObject();
            day.put("day", dayIndex + 1);
            day.put("date", date.toString());
            PlanContextItem themeItem = pick(spots, dayIndex);
            if (themeItem == null) {
                themeItem = pick(items, dayIndex);
            }
            String dayTheme = themeItem == null
                    ? (dayIndex == 0 ? "初见这座城市" : "城市慢漫游")
                    : themeItem.getCategoryName() + "的一天";
            day.put("theme", dayTheme);
            day.put("summary", "围绕" + dayTheme + "安排，上午轻松出发，中午留出吃饭时间，下午看一处人文或街区，傍晚收尾。");

            ArrayNode timeline = day.putArray("timeline");
            for (int i = 0; i < TIMES.length; i++) {
                PlanContextItem item = switch (i) {
                    case 0 -> pick(spots, dayIndex);
                    case 1 -> pick(foods, dayIndex);
                    case 2 -> pick(experiences, dayIndex);
                    default -> pick(spots, dayIndex + 1);
                };
                if (item == null) {
                    item = pick(items, dayIndex * 4 + i);
                }
                ObjectNode node = timeline.addObject();
                node.put("time", TIMES[i]);
                node.put("durationMinutes", i == 1 ? 75 : 90);
                node.put("type", typeOf(item, i));
                node.put("title", item == null ? GENERIC_TITLES[i] : item.getTitle());
                node.put("area", item == null ? "" : item.getArea());
                node.put("address", "");
                String description = item == null
                        ? "沿着城市的主街慢慢走，留意路边的老建筑与小店，把节奏放慢，感受当地的日常。"
                        : item.getSummary();
                if (description == null || description.length() < 10) {
                    description = "在这里停留一会儿，看看周围的生活，再决定下一站去哪里。";
                }
                node.put("description", description);
                node.put("tips", "建议错峰前往，具体开放时间与价格以现场为准。");
                if (item != null && item.getId() != null) {
                    node.put("sourceContentId", item.getId());
                }
            }

            ArrayNode food = day.putArray("food");
            if (foods.isEmpty()) {
                ObjectNode foodNode = food.addObject();
                foodNode.put("name", plan.getCityName() + "本地风味小馆");
                foodNode.put("reason", "安排在中午，给下午留出散步时间");
                foodNode.put("priceRange", "人均 ¥60-120");
            } else {
                for (int i = 0; i < Math.min(2, foods.size()); i++) {
                    PlanContextItem item = foods.get((dayIndex + i) % foods.size());
                    ObjectNode foodNode = food.addObject();
                    foodNode.put("name", item.getTitle());
                    foodNode.put("reason", "本地内容推荐，适合安排午餐或晚餐");
                    foodNode.put("priceRange", item.getPriceRange() == null ? "丰俭由人" : item.getPriceRange());
                    if (item.getId() != null) {
                        foodNode.put("sourceContentId", item.getId());
                    }
                }
            }

            ArrayNode transport = day.putArray("transport");
            ObjectNode route = transport.addObject();
            route.put("from", timeline.get(0).path("title").asText());
            route.put("to", timeline.get(1).path("title").asText());
            route.put("method", "步行 / 打车");
            route.put("duration", "约 15-25 分钟");
            route.put("tip", "距离较近可步行，带老人或孩子建议打车。");
        }

        ObjectNode budget = root.putObject("budget");
        budget.put("level", "人均 ¥" + (plan.getDayCount() * 300) + "-" + (plan.getDayCount() * 500));
        ArrayNode breakdown = budget.putArray("breakdown");
        breakdown.addObject().put("item", "餐饮").put("amount", "¥" + (plan.getDayCount() * 150) + "-" + (plan.getDayCount() * 250));
        breakdown.addObject().put("item", "交通").put("amount", "¥" + (plan.getDayCount() * 40) + "-" + (plan.getDayCount() * 80));
        breakdown.addObject().put("item", "门票/体验").put("amount", "¥" + (plan.getDayCount() * 60) + "-" + (plan.getDayCount() * 150));

        root.put("rainPlan", "如遇下雨，可把户外点位替换为博物馆、茶馆或商场，行程整体顺延，不必勉强赶路。");
        ArrayNode packing = root.putArray("packingTips");
        packing.add("舒适的鞋");
        packing.add("雨具");
        packing.add("充电宝");
        ArrayNode warnings = root.putArray("warnings");
        warnings.add("具体开放时间、预约政策以官方公告为准。");
        warnings.add("节假日人流较多，建议提前规划交通。");

        return new GeneratedPlan(root, "mock-plan-v1", 0L, "内置模板生成器");
    }

    private ObjectNode transportLeg(TravelPlan plan, String from, String to, String date, boolean outbound) {
        ObjectNode leg = objectMapper.createObjectNode();
        String preference = plan.getTransportPreference() == null ? "AUTO" : plan.getTransportPreference();
        String mode = switch (preference) {
            case "TRAIN" -> "TRAIN";
            case "FLIGHT" -> "FLIGHT";
            case "SELF_DRIVE" -> "SELF_DRIVE";
            default -> "MIXED";
        };
        leg.put("date", date);
        leg.put("mode", mode);
        leg.put("title", from + " → " + to);
        leg.put("duration", "SELF_DRIVE".equals(preference)
                ? "按导航预估约 8-14 小时（不含休息）"
                : "高铁约 4-7 小时 / 航班约 2-3 小时（含往返机场）");
        leg.put("departureWindow", outbound ? "07:00-09:00" : "16:00-18:00");
        leg.put("arrivalWindow", outbound ? "15:00-19:00" : "21:00-23:00");
        ArrayNode options = leg.putArray("options");
        if ("SELF_DRIVE".equals(preference)) {
            options.addObject()
                    .put("type", "自驾")
                    .put("label", outbound ? "全程自驾 · 早出发" : "全程自驾 · 午后返程")
                    .put("note", "建议每 2 小时进服务区休息，具体路线以导航为准");
            leg.put("tip", outbound
                    ? "出发前加满油、检查胎压，尽量避开早高峰出城。"
                    : "返程前预留加油和休息时间，避免夜间疲劳驾驶。");
        }
        if (!"FLIGHT".equals(preference) && !"SELF_DRIVE".equals(preference)) {
            options.addObject()
                    .put("type", "高铁")
                    .put("label", outbound ? "G 字头早班" : "G 字头傍晚班")
                    .put("note", "建议提前 30 分钟到站，具体车次与票价以 12306 为准");
        }
        if (!"TRAIN".equals(preference) && !"SELF_DRIVE".equals(preference)) {
            options.addObject()
                    .put("type", "航班")
                    .put("label", outbound ? "早班直飞" : "傍晚直飞")
                    .put("note", "建议提前 2 小时到机场，具体航班以航司为准");
        }
        if (!"SELF_DRIVE".equals(preference)) {
            leg.put("tip", outbound
                    ? "优先选择上午出发，给抵达后的入住和第一段行程留出时间。"
                    : "返程建议留出 2 小时以上缓冲，避免行程收尾太赶。");
        }
        return leg;
    }

    private ObjectNode drivingInfo(TravelPlan plan) {
        ObjectNode driving = objectMapper.createObjectNode();
        driving.put("route", plan.getOriginCityName() + " → " + plan.getCityName());
        driving.put("distance", "约 800-1,300 km（以导航实际里程为准）");
        driving.put("duration", "约 9-15 小时（不含休息与堵车）");
        driving.put("tolls", "约 ¥400-700（以实际路段为准）");
        driving.put("fuel", "约 ¥500-900（按车型与油价估算）");
        driving.put("parking", "酒店与景区停车位以现场为准，建议提前电话确认");
        ArrayNode restStops = driving.putArray("restStops");
        restStops.add("建议每 2 小时进入服务区休息 15-20 分钟");
        restStops.add("长途可在途经城市安排一晚中转，避免疲劳驾驶");
        ArrayNode tips = driving.putArray("tips");
        tips.add("出发前检查胎压、油量、刹车与应急工具");
        tips.add("节假日尽量错峰出发，关注高速拥堵与天气预警");
        return driving;
    }

    private PlanContextItem pick(List<PlanContextItem> list, int index) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(Math.floorMod(index, list.size()));
    }

    private String typeOf(PlanContextItem item, int index) {
        if (item == null) {
            return switch (index) {
                case 1 -> "FOOD";
                case 2 -> "EXPERIENCE";
                default -> "SPOT";
            };
        }
        return switch (item.getCategoryCode() == null ? "" : item.getCategoryCode()) {
            case "FOOD" -> "FOOD";
            case "SHOP" -> "SHOP";
            case "INTANGIBLE", "PLAY" -> "EXPERIENCE";
            case "STAY" -> "REST";
            case "SPOT", "HERITAGE", "FIGURE", "FOLK" -> "SPOT";
            default -> "OTHER";
        };
    }

    private List<String> parsePreferences(TravelPlan plan) {
        List<String> result = new ArrayList<>();
        if (plan.getPreferences() == null || plan.getPreferences().isBlank()) {
            return result;
        }
        try {
            result.addAll(objectMapper.readValue(plan.getPreferences(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)));
        } catch (Exception ignored) {
            // 忽略格式异常，偏好只用于展示
        }
        return result;
    }
}
