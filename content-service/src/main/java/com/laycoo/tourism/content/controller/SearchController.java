package com.laycoo.tourism.content.controller;

import com.laycoo.tourism.common.api.ApiResponse;
import com.laycoo.tourism.common.api.PageResult;
import com.laycoo.tourism.common.dto.ContentCard;
import com.laycoo.tourism.content.dto.ContentQuery;
import com.laycoo.tourism.content.service.ContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final ContentService contentService;

    @GetMapping
    public ApiResponse<PageResult<ContentCard>> search(@ModelAttribute ContentQuery query,
                                                       @RequestParam(required = false) String keyword) {
        if (keyword != null) {
            query.setKeyword(keyword);
        }
        if (query.getKeyword() == null || query.getKeyword().isBlank()) {
            return ApiResponse.ok(PageResult.empty(Math.max(query.getPage(), 1), Math.max(query.getSize(), 1)));
        }
        return ApiResponse.ok(contentService.list(query, false));
    }

    @GetMapping("/hot")
    public ApiResponse<List<String>> hotKeywords() {
        return ApiResponse.ok(contentService.hotKeywords());
    }
}
