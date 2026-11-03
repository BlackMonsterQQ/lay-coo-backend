package com.laycoo.tourism.ai.plan.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;

/**
 * 对 AI 返回的行程 JSON 做结构与基本事实校验，避免脏数据直接进入前端。
 */
@Component
public class PlanValidator {

    private static final Set<String> TYPES = Set.of(
            "SPOT", "FOOD", "EXPERIENCE", "SHOP", "REST", "OTHER");

    public void validate(JsonNode plan, int dayCount, LocalDate startDate, String transportPreference) {
        if (plan == null || !plan.isObject()) {
            throw new PlanGenerationException("AI 返回的攻略不是 JSON 对象");
        }
        String title = text(plan, "title");
        if (title.length() < 6 || title.length() > 80) {
            throw new PlanGenerationException("攻略标题长度不合法");
        }
        String summary = text(plan, "summary");
        if (summary.length() < 20) {
            throw new PlanGenerationException("攻略摘要过短");
        }
        if (plan.toString().toLowerCase().contains("<script")
                || plan.toString().toLowerCase().contains("<iframe")) {
            throw new PlanGenerationException("攻略包含不安全 HTML 内容");
        }
        validateIntercity(plan, startDate, dayCount);
        if ("SELF_DRIVE".equalsIgnoreCase(transportPreference)) {
            validateDriving(plan);
        }

        JsonNode days = plan.get("days");
        if (days == null || !days.isArray() || days.size() != dayCount) {
            throw new PlanGenerationException("行程天数与请求不一致");
        }
        int totalNodes = 0;
        for (int i = 0; i < days.size(); i++) {
            JsonNode day = days.get(i);
            if (day == null || !day.isObject()) {
                throw new PlanGenerationException("第 " + (i + 1) + " 天数据格式错误");
            }
            String date = text(day, "date");
            if (!startDate.plusDays(i).toString().equals(date)) {
                throw new PlanGenerationException("第 " + (i + 1) + " 天日期不连续");
            }
            if (!StringUtils.hasText(text(day, "theme"))) {
                throw new PlanGenerationException("第 " + (i + 1) + " 天缺少主题");
            }
            String dayStartText = text(day, "startTime");
            String dayEndText = text(day, "endTime");
            if (!StringUtils.hasText(dayStartText) || !StringUtils.hasText(dayEndText)
                    || !StringUtils.hasText(text(day, "startPoint"))
                    || !StringUtils.hasText(text(day, "routeSummary"))) {
                throw new PlanGenerationException("第 " + (i + 1) + " 天缺少出发/结束时间或路线说明");
            }
            LocalTime dayStart = parseTime(dayStartText, "第 " + (i + 1) + " 天出发时间");
            LocalTime dayEnd = parseTime(dayEndText, "第 " + (i + 1) + " 天结束时间");
            if (!dayEnd.isAfter(dayStart)) {
                throw new PlanGenerationException("第 " + (i + 1) + " 天结束时间必须晚于出发时间");
            }
            JsonNode timeline = day.get("timeline");
            if (timeline == null || !timeline.isArray() || timeline.size() < 3 || timeline.size() > 7) {
                throw new PlanGenerationException("第 " + (i + 1) + " 天安排数量不合法，至少要有 3 个具体安排");
            }
            totalNodes += timeline.size();
            LocalTime previous = null;
            LocalTime previousEnd = null;
            boolean firstNode = true;
            boolean hasFood = false;
            boolean hasPlace = false;
            LocalTime lastEnd = dayStart;
            for (JsonNode item : timeline) {
                String time = text(item, "time");
                LocalTime parsed = parseTime(time, "行程时间");
                String departureText = text(item, "departureTime");
                String arrivalText = text(item, "arrivalTime");
                if (!StringUtils.hasText(departureText) || !StringUtils.hasText(arrivalText)) {
                    throw new PlanGenerationException("行程节点缺少出发/到达时间");
                }
                LocalTime departure = parseTime(departureText, "出发时间");
                LocalTime arrival = parseTime(arrivalText, "到达时间");
                if (departure.isAfter(arrival)) {
                    throw new PlanGenerationException("行程节点出发时间不能晚于到达时间");
                }
                if (!arrival.equals(parsed)) {
                    throw new PlanGenerationException("行程节点到达时间与 time 不一致");
                }
                if (firstNode && !departure.equals(dayStart)) {
                    throw new PlanGenerationException("第 " + (i + 1) + " 天第一个节点出发时间必须等于 startTime");
                }
                if (!StringUtils.hasText(text(item, "from"))
                        || !StringUtils.hasText(text(item, "transport"))) {
                    throw new PlanGenerationException("行程节点缺少出发地或交通方式");
                }
                if (!item.has("travelMinutes") || !item.has("durationMinutes")) {
                    throw new PlanGenerationException("行程节点缺少交通耗时或停留时长");
                }
                int travelMinutes = item.path("travelMinutes").asInt(-1);
                int duration = item.path("durationMinutes").asInt(0);
                if (travelMinutes < 0 || duration <= 0) {
                    throw new PlanGenerationException("行程节点时长不合法");
                }
                if (previousEnd != null && departure.isBefore(previousEnd)) {
                    throw new PlanGenerationException("第 " + (i + 1) + " 天行程时间发生重叠");
                }
                if (previous != null && !parsed.isAfter(previous)) {
                    throw new PlanGenerationException("第 " + (i + 1) + " 天时间顺序不正确");
                }
                previous = parsed;
                previousEnd = arrival.plusMinutes(duration);
                lastEnd = previousEnd;
                if (!StringUtils.hasText(text(item, "title"))) {
                    throw new PlanGenerationException("行程节点缺少标题");
                }
                String type = text(item, "type");
                if (!TYPES.contains(type)) {
                    throw new PlanGenerationException("行程节点类型不合法: " + type);
                }
                if (text(item, "description").length() < 10) {
                    throw new PlanGenerationException("行程节点描述过短");
                }
                hasFood = hasFood || "FOOD".equals(type);
                hasPlace = hasPlace || "SPOT".equals(type) || "EXPERIENCE".equals(type);
                firstNode = false;
            }
            if (lastEnd.isAfter(dayEnd)) {
                throw new PlanGenerationException("第 " + (i + 1) + " 天最后一个安排超过结束时间");
            }
            if (!hasFood || !hasPlace) {
                throw new PlanGenerationException("第 " + (i + 1) + " 天需要同时包含具体景点/体验和餐饮安排");
            }
            JsonNode foods = day.get("food");
            if (foods == null || !foods.isArray() || foods.isEmpty()) {
                throw new PlanGenerationException("第 " + (i + 1) + " 天缺少具体餐饮推荐");
            }
            for (JsonNode food : foods) {
                if (!StringUtils.hasText(text(food, "name"))) {
                    throw new PlanGenerationException("第 " + (i + 1) + " 天餐饮推荐缺少店名/菜名");
                }
            }
        }
        if (totalNodes > 56) {
            throw new PlanGenerationException("行程节点过多");
        }
    }

    private LocalTime parseTime(String value, String label) {
        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException e) {
            throw new PlanGenerationException(label + "格式错误: " + value);
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? "" : value.asText("").trim();
    }

    private void validateIntercity(JsonNode plan, LocalDate startDate, int dayCount) {
        JsonNode transport = plan.get("intercityTransport");
        if (transport == null || !transport.isObject()) {
            throw new PlanGenerationException("缺少出发地与目的地之间的交通方案");
        }
        validateTransportLeg(transport.get("outbound"), startDate, "去程");
        validateTransportLeg(transport.get("return"),
                startDate.plusDays(Math.max(0, dayCount - 1)), "返程");
        String note = text(transport, "note");
        if (note.length() < 8) {
            throw new PlanGenerationException("交通方案缺少实时信息提示");
        }
    }

    private void validateDriving(JsonNode plan) {
        JsonNode driving = plan.get("driving");
        if (driving == null || !driving.isObject()) {
            throw new PlanGenerationException("自驾方案缺少路线信息");
        }
        if (!StringUtils.hasText(text(driving, "route"))
                || !StringUtils.hasText(text(driving, "distance"))
                || !StringUtils.hasText(text(driving, "duration"))
                || !StringUtils.hasText(text(driving, "parking"))) {
            throw new PlanGenerationException("自驾路线信息不完整");
        }
        JsonNode restStops = driving.get("restStops");
        if (restStops == null || !restStops.isArray() || restStops.isEmpty()) {
            throw new PlanGenerationException("自驾路线缺少休息/中转建议");
        }
    }

    private void validateTransportLeg(JsonNode leg, LocalDate expectedDate, String label) {
        if (leg == null || !leg.isObject()) {
            throw new PlanGenerationException(label + "交通方案格式错误");
        }
        if (!expectedDate.toString().equals(text(leg, "date"))) {
            throw new PlanGenerationException(label + "日期不正确");
        }
        String mode = text(leg, "mode");
        if (!Set.of("TRAIN", "FLIGHT", "MIXED", "SELF_DRIVE").contains(mode)) {
            throw new PlanGenerationException(label + "交通方式不合法");
        }
        if (!StringUtils.hasText(text(leg, "title")) || !StringUtils.hasText(text(leg, "duration"))
                || !StringUtils.hasText(text(leg, "departureWindow"))
                || !StringUtils.hasText(text(leg, "arrivalWindow"))) {
            throw new PlanGenerationException(label + "交通方案信息不完整");
        }
        JsonNode options = leg.get("options");
        if (options == null || !options.isArray() || options.isEmpty() || options.size() > 4) {
            throw new PlanGenerationException(label + "交通班次选项不合法");
        }
        for (JsonNode option : options) {
            if (!StringUtils.hasText(text(option, "type"))
                    || !StringUtils.hasText(text(option, "label"))) {
                throw new PlanGenerationException(label + "交通班次选项缺少信息");
            }
        }
    }
}
