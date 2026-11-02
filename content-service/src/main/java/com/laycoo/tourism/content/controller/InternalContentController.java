package com.laycoo.tourism.content.controller;

import com.laycoo.tourism.common.api.ApiResponse;
import com.laycoo.tourism.common.dto.ContentCard;
import com.laycoo.tourism.content.dto.ContentDetailVO;
import com.laycoo.tourism.content.dto.InternalCreateRequest;
import com.laycoo.tourism.content.dto.StatsRequest;
import com.laycoo.tourism.content.service.ContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 服务间内部接口，网关不对外暴露 /internal 前缀。
 */
@RestController
@RequestMapping("/internal/content")
@RequiredArgsConstructor
public class InternalContentController {

    private final ContentService contentService;

    @PostMapping
    public ApiResponse<Long> createFromAi(@Valid @RequestBody InternalCreateRequest request) {
        return ApiResponse.ok(contentService.createFromAi(request));
    }

    @GetMapping("/candidates")
    public ApiResponse<List<ContentCard>> candidates(@RequestParam(defaultValue = "200") int limit) {
        return ApiResponse.ok(contentService.candidates(limit));
    }

    @GetMapping("/{id}")
    public ApiResponse<ContentDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(contentService.adminDetail(id));
    }

    @PostMapping("/batch")
    public ApiResponse<List<ContentCard>> batch(@RequestBody List<Long> ids) {
        return ApiResponse.ok(contentService.cardsByIds(ids));
    }

    @PostMapping("/{id}/stats")
    public ApiResponse<Void> stats(@PathVariable Long id, @RequestBody StatsRequest request) {
        contentService.applyStats(id, request);
        return ApiResponse.ok();
    }
}
