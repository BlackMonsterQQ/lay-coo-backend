package com.laycoo.tourism.recommend.controller;

import com.laycoo.tourism.common.api.ApiResponse;
import com.laycoo.tourism.common.security.UserContext;
import com.laycoo.tourism.recommend.dto.RecommendItem;
import com.laycoo.tourism.recommend.service.RecommendService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recommend")
@RequiredArgsConstructor
public class RecommendController {

    private final RecommendService recommendService;

    @GetMapping("/home")
    public ApiResponse<List<RecommendItem>> home(@RequestParam(defaultValue = "8") int size) {
        return ApiResponse.ok(recommendService.home(UserContext.getUserId(), size));
    }

    @GetMapping("/hot")
    public ApiResponse<List<RecommendItem>> hot(@RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.ok(recommendService.hot(limit));
    }

    @GetMapping("/similar/{contentId}")
    public ApiResponse<List<RecommendItem>> similar(@PathVariable Long contentId,
                                                    @RequestParam(defaultValue = "6") int size) {
        return ApiResponse.ok(recommendService.similar(contentId, size));
    }
}
