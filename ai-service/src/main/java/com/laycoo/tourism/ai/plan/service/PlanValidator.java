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

    public void validate(JsonNode plan, int dayCount, LocalDate startDate) {
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
            JsonNode timeline = day.get("timeline");
            if (timeline == null || !timeline.isArray() || timeline.size() < 2 || timeline.size() > 8) {
                throw new PlanGenerationException("第 " + (i + 1) + " 天安排数量不合法");
            }
            totalNodes += timeline.size();
            LocalTime previous = null;
            for (JsonNode item : timeline) {
                String time = text(item, "time");
                LocalTime parsed;
                try {
                    parsed = LocalTime.parse(time);
                } catch (DateTimeParseException e) {
                    throw new PlanGenerationException("行程时间格式错误: " + time);
                }
                if (previous != null && !parsed.isAfter(previous)) {
                    throw new PlanGenerationException("第 " + (i + 1) + " 天时间顺序不正确");
                }
                previous = parsed;
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
            }
        }
        if (totalNodes > 56) {
            throw new PlanGenerationException("行程节点过多");
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

    private void validateTransportLeg(JsonNode leg, LocalDate expectedDate, String label) {
        if (leg == null || !leg.isObject()) {
            throw new PlanGenerationException(label + "交通方案格式错误");
        }
        if (!expectedDate.toString().equals(text(leg, "date"))) {
            throw new PlanGenerationException(label + "日期不正确");
        }
        String mode = text(leg, "mode");
        if (!Set.of("TRAIN", "FLIGHT", "MIXED").contains(mode)) {
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
