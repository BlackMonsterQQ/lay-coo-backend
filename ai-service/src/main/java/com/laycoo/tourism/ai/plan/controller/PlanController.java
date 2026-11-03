package com.laycoo.tourism.ai.plan.controller;

import com.laycoo.tourism.ai.plan.dto.PlanDetailResponse;
import com.laycoo.tourism.ai.plan.dto.PlanGenerateRequest;
import com.laycoo.tourism.ai.plan.dto.PlanGenerateResponse;
import com.laycoo.tourism.ai.plan.service.PlanService;
import com.laycoo.tourism.common.api.ApiResponse;
import com.laycoo.tourism.common.security.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/plan")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    @PostMapping("/generate")
    public ApiResponse<PlanGenerateResponse> generate(@Valid @RequestBody PlanGenerateRequest request,
                                                      HttpServletRequest httpRequest) {
        return ApiResponse.ok(planService.create(request, UserContext.getUserId(), clientIp(httpRequest)));
    }

    @GetMapping("/{planNo}")
    public ApiResponse<PlanDetailResponse> detail(@PathVariable String planNo) {
        return ApiResponse.ok(planService.detail(planNo));
    }

    @PostMapping("/{planNo}/retry")
    public ApiResponse<PlanDetailResponse> retry(@PathVariable String planNo,
                                                 HttpServletRequest httpRequest) {
        return ApiResponse.ok(planService.retry(planNo, UserContext.getUserId(), clientIp(httpRequest)));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
