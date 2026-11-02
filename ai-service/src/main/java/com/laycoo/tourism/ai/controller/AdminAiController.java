package com.laycoo.tourism.ai.controller;

import com.laycoo.tourism.ai.dto.CreateTaskRequest;
import com.laycoo.tourism.ai.entity.AiTask;
import com.laycoo.tourism.ai.service.AiTaskService;
import com.laycoo.tourism.common.api.ApiResponse;
import com.laycoo.tourism.common.api.PageResult;
import com.laycoo.tourism.common.security.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/ai")
@RequiredArgsConstructor
public class AdminAiController {

    private final AiTaskService taskService;

    @PostMapping("/task")
    public ApiResponse<AiTask> create(@Valid @RequestBody CreateTaskRequest request) {
        UserContext.requireAdmin();
        return ApiResponse.ok(taskService.create(request));
    }

    @GetMapping("/task/list")
    public ApiResponse<PageResult<AiTask>> list(@RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "10") long size,
                                                @RequestParam(required = false) String status) {
        UserContext.requireAdmin();
        return ApiResponse.ok(taskService.list(page, size, status));
    }

    @GetMapping("/task/stats")
    public ApiResponse<Map<String, Object>> stats() {
        UserContext.requireAdmin();
        return ApiResponse.ok(taskService.stats());
    }

    @GetMapping("/task/{id}")
    public ApiResponse<AiTask> detail(@PathVariable Long id) {
        UserContext.requireAdmin();
        return ApiResponse.ok(taskService.detail(id));
    }

    @PostMapping("/task/{id}/retry")
    public ApiResponse<AiTask> retry(@PathVariable Long id) {
        UserContext.requireAdmin();
        return ApiResponse.ok(taskService.retry(id));
    }
}
