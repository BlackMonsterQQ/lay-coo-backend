package com.laycoo.tourism.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laycoo.tourism.ai.config.AiProperties;
import com.laycoo.tourism.ai.dto.AiContent;
import com.laycoo.tourism.ai.dto.CreateTaskRequest;
import com.laycoo.tourism.ai.dto.GeneratedResult;
import com.laycoo.tourism.ai.entity.AiTask;
import com.laycoo.tourism.ai.mapper.AiTaskMapper;
import com.laycoo.tourism.common.api.PageResult;
import com.laycoo.tourism.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AiTaskService {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";

    private final AiTaskMapper taskMapper;
    private final AiProperties properties;
    private final MockContentGenerator mockContentGenerator;
    private final LlmContentGenerator llmContentGenerator;
    private final ContentModerator moderator;
    private final ContentPublisher publisher;
    private final ObjectMapper objectMapper;
    private final TaskExecutor taskExecutor;

    public AiTaskService(AiTaskMapper taskMapper,
                         AiProperties properties,
                         MockContentGenerator mockContentGenerator,
                         LlmContentGenerator llmContentGenerator,
                         ContentModerator moderator,
                         ContentPublisher publisher,
                         ObjectMapper objectMapper,
                         @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor) {
        this.taskMapper = taskMapper;
        this.properties = properties;
        this.mockContentGenerator = mockContentGenerator;
        this.llmContentGenerator = llmContentGenerator;
        this.moderator = moderator;
        this.publisher = publisher;
        this.objectMapper = objectMapper;
        this.taskExecutor = taskExecutor;
    }

    public AiTask create(CreateTaskRequest request) {
        AiTask task = new AiTask();
        task.setCategoryCode(request.getCategoryCode());
        task.setArea(request.getArea());
        task.setTargetCount(Math.min(Math.max(request.getCount(), 1), properties.getCountLimit()));
        task.setKeywords(request.getKeywords());
        task.setStatus(STATUS_PENDING);
        task.setModel(properties.llmEnabled() ? properties.getModel() : "mock-tourism-v1");
        task.setCostTokens(0L);
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        taskMapper.insert(task);
        asyncRun(task.getId());
        return task;
    }

    public AiTask retry(Long id) {
        AiTask task = require(id);
        if (STATUS_RUNNING.equals(task.getStatus())) {
            throw new BusinessException("任务正在执行中，请稍后再试");
        }
        task.setStatus(STATUS_PENDING);
        task.setError(null);
        task.setResult(null);
        task.setUpdateTime(LocalDateTime.now());
        taskMapper.updateById(task);
        asyncRun(id);
        return task;
    }

    public AiTask detail(Long id) {
        return require(id);
    }

    public PageResult<AiTask> list(long page, long size, String status) {
        long current = Math.max(page, 1);
        long pageSize = Math.min(Math.max(size, 1), 50);
        LambdaQueryWrapper<AiTask> wrapper = new LambdaQueryWrapper<AiTask>()
                .orderByDesc(AiTask::getId);
        if (StringUtils.hasText(status)) {
            wrapper.eq(AiTask::getStatus, status);
        }
        Page<AiTask> result = taskMapper.selectPage(new Page<>(current, pageSize), wrapper);
        for (AiTask task : result.getRecords()) {
            // 列表不返回超长 prompt
            task.setPrompt(null);
        }
        return PageResult.of(result.getRecords(), result.getTotal(), current, pageSize);
    }

    public Map<String, Object> stats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", taskMapper.selectCount(null));
        for (String status : List.of(STATUS_PENDING, STATUS_RUNNING, STATUS_SUCCESS, STATUS_FAILED)) {
            stats.put(status.toLowerCase(),
                    taskMapper.selectCount(new LambdaQueryWrapper<AiTask>().eq(AiTask::getStatus, status)));
        }
        long tokens = 0;
        for (AiTask task : taskMapper.selectList(null)) {
            tokens += task.getCostTokens() == null ? 0 : task.getCostTokens();
        }
        stats.put("costTokens", tokens);
        stats.put("model", properties.llmEnabled() ? properties.getModel() : "mock-tourism-v1");
        stats.put("llmEnabled", properties.llmEnabled());
        stats.put("provider", properties.getProvider());
        return stats;
    }

    private void asyncRun(Long taskId) {
        taskExecutor.execute(() -> run(taskId));
    }

    public void run(Long taskId) {
        AiTask task = taskMapper.selectById(taskId);
        if (task == null) {
            return;
        }
        task.setStatus(STATUS_RUNNING);
        task.setUpdateTime(LocalDateTime.now());
        taskMapper.updateById(task);

        long startedAt = System.currentTimeMillis();
        try {
            GeneratedResult result = properties.llmEnabled()
                    ? llmContentGenerator.generate(task)
                    : mockContentGenerator.generate(task);

            task.setPrompt(truncate(result.getPrompt(), 4000));
            task.setModel(result.getModel());
            task.setCostTokens(result.getTokens());

            List<Long> created = new ArrayList<>();
            List<String> rejected = new ArrayList<>();
            for (AiContent content : result.getContents()) {
                if (!StringUtils.hasText(content.getCategoryCode())) {
                    content.setCategoryCode(task.getCategoryCode());
                }
                if (!StringUtils.hasText(content.getArea())) {
                    content.setArea(task.getArea());
                }
                content.setAiTaskId(task.getId());
                String reason = moderator.check(content);
                if (reason != null) {
                    rejected.add(safeTitle(content) + "：" + reason);
                    continue;
                }
                try {
                    created.add(publisher.publish(content));
                } catch (Exception e) {
                    rejected.add(safeTitle(content) + "：入库失败（" + e.getMessage() + "）");
                }
            }

            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("created", created.size());
            summary.put("contentIds", created);
            summary.put("rejected", rejected);
            summary.put("durationMs", System.currentTimeMillis() - startedAt);
            summary.put("model", result.getModel());
            task.setResult(objectMapper.writeValueAsString(summary));

            if (created.isEmpty()) {
                task.setStatus(STATUS_FAILED);
                task.setError("生成内容未通过审核或入库失败：" + String.join("；", rejected));
            } else {
                task.setStatus(STATUS_SUCCESS);
                task.setError(rejected.isEmpty() ? null : "部分内容被拦截：" + String.join("；", rejected));
            }
            log.info("AI 任务 {} 完成：入库 {} 条，拦截 {} 条", taskId, created.size(), rejected.size());
        } catch (Exception e) {
            log.error("AI 任务 {} 执行失败", taskId, e);
            task.setStatus(STATUS_FAILED);
            task.setError(truncate(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(), 500));
        } finally {
            task.setUpdateTime(LocalDateTime.now());
            taskMapper.updateById(task);
        }
    }

    private AiTask require(Long id) {
        AiTask task = taskMapper.selectById(id);
        if (task == null) {
            throw BusinessException.notFound("任务不存在");
        }
        return task;
    }

    private String safeTitle(AiContent content) {
        return StringUtils.hasText(content.getTitle()) ? content.getTitle() : "未命名内容";
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
