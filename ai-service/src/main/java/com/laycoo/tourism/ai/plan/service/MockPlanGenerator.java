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
