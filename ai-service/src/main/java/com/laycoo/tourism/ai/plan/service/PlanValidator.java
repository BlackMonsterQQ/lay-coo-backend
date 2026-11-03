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
}
