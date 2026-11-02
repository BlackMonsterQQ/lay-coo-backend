package com.laycoo.tourism.interaction.controller;

import com.laycoo.tourism.common.api.ApiResponse;
import com.laycoo.tourism.common.api.PageResult;
import com.laycoo.tourism.common.dto.ContentCard;
import com.laycoo.tourism.common.security.UserContext;
import com.laycoo.tourism.interaction.dto.ContentActionRequest;
import com.laycoo.tourism.interaction.service.FavoriteService;
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
@RequestMapping("/api/v1/favorite")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping
    public ApiResponse<Map<String, Object>> add(@Valid @RequestBody ContentActionRequest request) {
        Long userId = UserContext.requireUserId();
        favoriteService.add(userId, request.getContentId());
        return ApiResponse.ok(state(userId, request.getContentId()));
    }

    @DeleteMapping("/{contentId}")
    public ApiResponse<Map<String, Object>> remove(@PathVariable Long contentId) {
        Long userId = UserContext.requireUserId();
        favoriteService.remove(userId, contentId);
        return ApiResponse.ok(state(userId, contentId));
    }

    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status(@RequestParam Long contentId) {
        Long userId = UserContext.requireUserId();
        return ApiResponse.ok(state(userId, contentId));
    }

    private Map<String, Object> state(Long userId, Long contentId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("favorited", favoriteService.status(userId, contentId));
        result.put("count", favoriteService.count(contentId));
        return result;
    }

    @GetMapping("/list")
    public ApiResponse<PageResult<ContentCard>> list(@RequestParam(defaultValue = "1") long page,
                                                     @RequestParam(defaultValue = "12") long size) {
        return ApiResponse.ok(favoriteService.list(UserContext.requireUserId(), page, size));
    }
}
