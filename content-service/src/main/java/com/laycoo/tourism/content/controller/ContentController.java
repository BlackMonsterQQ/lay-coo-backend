package com.laycoo.tourism.content.controller;

import com.laycoo.tourism.common.api.ApiResponse;
import com.laycoo.tourism.common.api.PageResult;
import com.laycoo.tourism.common.dto.ContentCard;
import com.laycoo.tourism.content.dto.ContentDetailVO;
import com.laycoo.tourism.content.dto.ContentQuery;
import com.laycoo.tourism.content.service.ContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/content")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;

    @GetMapping("/list")
    public ApiResponse<PageResult<ContentCard>> list(@ModelAttribute ContentQuery query) {
        return ApiResponse.ok(contentService.list(query, false));
    }

    @GetMapping("/areas")
    public ApiResponse<List<String>> areas() {
        return ApiResponse.ok(contentService.areas());
    }

    @GetMapping("/{id}")
    public ApiResponse<ContentDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(contentService.detail(id, true));
    }

    @GetMapping("/{id}/related")
    public ApiResponse<List<ContentCard>> related(@PathVariable Long id,
                                                  @RequestParam(defaultValue = "6") int size) {
        return ApiResponse.ok(contentService.related(id, size));
    }
}
