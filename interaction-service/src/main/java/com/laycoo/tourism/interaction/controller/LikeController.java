package com.laycoo.tourism.interaction.controller;

import com.laycoo.tourism.common.api.ApiResponse;
import com.laycoo.tourism.common.security.UserContext;
import com.laycoo.tourism.interaction.dto.ContentActionRequest;
import com.laycoo.tourism.interaction.service.LikeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/like")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    @PostMapping
    public ApiResponse<Map<String, Object>> add(@Valid @RequestBody ContentActionRequest request) {
        Long userId = UserContext.requireUserId();
        likeService.add(userId, request.getContentId());
        return ApiResponse.ok(state(userId, request.getContentId()));
    }

    @DeleteMapping("/{contentId}")
    public ApiResponse<Map<String, Object>> remove(@PathVariable Long contentId) {
        Long userId = UserContext.requireUserId();
        likeService.remove(userId, contentId);
        return ApiResponse.ok(state(userId, contentId));
    }

    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status(@RequestParam Long contentId) {
        Long userId = UserContext.requireUserId();
        return ApiResponse.ok(state(userId, contentId));
    }

    private Map<String, Object> state(Long userId, Long contentId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("liked", likeService.status(userId, contentId));
        result.put("count", likeService.count(contentId));
        return result;
    }
}
