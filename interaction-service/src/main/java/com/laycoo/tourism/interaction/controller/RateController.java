package com.laycoo.tourism.interaction.controller;

import com.laycoo.tourism.common.api.ApiResponse;
import com.laycoo.tourism.common.security.UserContext;
import com.laycoo.tourism.interaction.dto.RateRequest;
import com.laycoo.tourism.interaction.service.RateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class RateController {

    private final RateService rateService;

    @PostMapping("/api/v1/rate")
    public ApiResponse<Map<String, Object>> rate(@Valid @RequestBody RateRequest request) {
        return ApiResponse.ok(rateService.rate(UserContext.requireUserId(), request));
    }

    @GetMapping("/api/v1/rate/summary")
    public ApiResponse<Map<String, Object>> summary(@RequestParam Long contentId) {
        return ApiResponse.ok(rateService.summary(contentId, UserContext.getUserId()));
    }
}
