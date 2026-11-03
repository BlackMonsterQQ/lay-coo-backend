package com.laycoo.tourism.ai.plan.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laycoo.tourism.ai.config.AiProperties;
import com.laycoo.tourism.ai.feign.ContentClient;
import com.laycoo.tourism.ai.plan.dto.GeneratedPlan;
import com.laycoo.tourism.ai.plan.dto.PlanContextItem;
import com.laycoo.tourism.ai.plan.entity.TravelPlan;
import com.laycoo.tourism.ai.plan.mapper.TravelPlanMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class PlanTaskService {

    private final TravelPlanMapper planMapper;
    private final ContentClient contentClient;
    private final AiProperties properties;
    private final MockPlanGenerator mockGenerator;
    private final DeepSeekPlanGenerator deepSeekGenerator;
    private final PlanValidator validator;
    private final ObjectMapper objectMapper;
    private final TaskExecutor taskExecutor;

    public PlanTaskService(TravelPlanMapper planMapper,
                           ContentClient contentClient,
                           AiProperties properties,
                           MockPlanGenerator mockGenerator,
                           DeepSeekPlanGenerator deepSeekGenerator,
                           PlanValidator validator,
                           ObjectMapper objectMapper,
                           @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor) {
        this.planMapper = planMapper;
        this.contentClient = contentClient;
        this.properties = properties;
        this.mockGenerator = mockGenerator;
        this.deepSeekGenerator = deepSeekGenerator;
        this.validator = validator;
        this.objectMapper = objectMapper;
        this.taskExecutor = taskExecutor;
    }

    public void runAsync(Long planId) {
        taskExecutor.execute(() -> run(planId));
    }

    public void run(Long planId) {
        TravelPlan plan = planMapper.selectById(planId);
        if (plan == null) {
            return;
        }
        plan.setStatus("RUNNING");
        plan.setProgress(10);
        plan.setError(null);
        plan.setUpdateTime(LocalDateTime.now());
        planMapper.updateById(plan);

        try {
            List<PlanContextItem> context = loadContext(plan);
            plan.setProgress(35);
            plan.setUpdateTime(LocalDateTime.now());
            planMapper.updateById(plan);

            GeneratedPlan generated;
            boolean fallback = false;
            if (properties.llmEnabled()) {
                try {
                    generated = deepSeekGenerator.generate(plan, context);
                } catch (Exception e) {
                    if (!properties.isPlanFallbackEnabled()) {
                        throw e;
                    }
                    log.warn("DeepSeek 生成失败，使用内置模板兜底: {}", e.getMessage());
                    generated = mockGenerator.generate(plan, context);
                    fallback = true;
                }
            } else {
                generated = mockGenerator.generate(plan, context);
            }

            validator.validate(generated.getPlan(), plan.getDayCount(), plan.getStartDate());

            JsonNode planJson = generated.getPlan();
            plan.setTitle(truncate(planJson.path("title").asText(""), 120));
            plan.setSummary(truncate(planJson.path("summary").asText(""), 1000));
            plan.setContentJson(planJson.toString());
            plan.setModel(fallback ? generated.getModel() + "-fallback" : generated.getModel());
            plan.setPrompt(truncate(generated.getPrompt(), 4000));
            plan.setCostTokens(generated.getTokens());
            plan.setStatus("SUCCESS");
            plan.setProgress(100);
            plan.setError(null);
            plan.setUpdateTime(LocalDateTime.now());
            planMapper.updateById(plan);
            log.info("行程生成完成 planNo={}, days={}, model={}, fallback={}",
                    plan.getPlanNo(), plan.getDayCount(), plan.getModel(), fallback);
        } catch (Exception e) {
            log.error("行程生成失败 planNo={}", plan.getPlanNo(), e);
            plan.setStatus("FAILED");
            plan.setProgress(0);
            plan.setError(truncate(e.getMessage() == null ? "生成失败" : e.getMessage(), 1000));
            plan.setUpdateTime(LocalDateTime.now());
            planMapper.updateById(plan);
        }
    }

    private List<PlanContextItem> loadContext(TravelPlan plan) {
        try {
            String preferences = String.join(",", parsePreferences(plan));
            var response = contentClient.planContext(plan.getCityCode(), preferences,
                    Math.min(plan.getDayCount() * 10, 40));
            if (response != null && response.getCode() == 0 && response.getData() != null) {
                return response.getData();
            }
            log.warn("内容上下文为空 city={}", plan.getCityCode());
        } catch (Exception e) {
            log.warn("获取行程内容上下文失败 city={}: {}", plan.getCityCode(), e.getMessage());
        }
        return Collections.emptyList();
    }

    private List<String> parsePreferences(TravelPlan plan) {
        if (!StringUtils.hasText(plan.getPreferences())) {
            return List.of();
        }
        try {
            return objectMapper.readValue(plan.getPreferences(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            return List.of();
        }
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
