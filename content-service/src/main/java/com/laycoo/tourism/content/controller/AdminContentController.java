package com.laycoo.tourism.content.controller;

import com.laycoo.tourism.common.api.ApiResponse;
import com.laycoo.tourism.common.api.PageResult;
import com.laycoo.tourism.common.dto.ContentCard;
import com.laycoo.tourism.common.security.UserContext;
import com.laycoo.tourism.content.dto.AuditRequest;
import com.laycoo.tourism.content.dto.ContentDetailVO;
import com.laycoo.tourism.content.dto.ContentQuery;
import com.laycoo.tourism.content.dto.ContentSaveRequest;
import com.laycoo.tourism.content.service.ContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/content")
@RequiredArgsConstructor
public class AdminContentController {

    private final ContentService contentService;

    @GetMapping("/list")
    public ApiResponse<PageResult<ContentCard>> list(@ModelAttribute ContentQuery query) {
        UserContext.requireAdmin();
        return ApiResponse.ok(contentService.list(query, true));
    }

    @GetMapping("/{id}")
    public ApiResponse<ContentDetailVO> detail(@PathVariable Long id) {
        UserContext.requireAdmin();
        return ApiResponse.ok(contentService.adminDetail(id));
    }

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        UserContext.requireAdmin();
        return ApiResponse.ok(contentService.adminStats());
    }

    @PostMapping
    public ApiResponse<Long> create(@Valid @RequestBody ContentSaveRequest request) {
        UserContext.requireAdmin();
        return ApiResponse.ok(contentService.createManual(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody ContentSaveRequest request) {
        UserContext.requireAdmin();
        contentService.update(id, request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        UserContext.requireAdmin();
        contentService.delete(id);
        return ApiResponse.ok();
    }

    @PostMapping("/audit")
    public ApiResponse<Void> audit(@Valid @RequestBody AuditRequest request) {
        UserContext.requireAdmin();
        contentService.audit(request);
        return ApiResponse.ok();
    }
}
