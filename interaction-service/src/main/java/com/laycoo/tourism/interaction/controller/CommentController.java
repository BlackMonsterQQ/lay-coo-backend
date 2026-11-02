package com.laycoo.tourism.interaction.controller;

import com.laycoo.tourism.common.api.ApiResponse;
import com.laycoo.tourism.common.api.PageResult;
import com.laycoo.tourism.common.security.UserContext;
import com.laycoo.tourism.interaction.dto.CommentRequest;
import com.laycoo.tourism.interaction.dto.CommentVO;
import com.laycoo.tourism.interaction.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/api/v1/comment")
    public ApiResponse<CommentVO> add(@Valid @RequestBody CommentRequest request) {
        return ApiResponse.ok(commentService.add(UserContext.requireUserId(), request));
    }

    @DeleteMapping("/api/v1/comment/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        Long userId = UserContext.requireUserId();
        commentService.delete(id, userId, UserContext.isAdmin());
        return ApiResponse.ok();
    }

    @GetMapping("/api/v1/interaction/comment/list")
    public ApiResponse<PageResult<CommentVO>> list(@RequestParam Long contentId,
                                                   @RequestParam(defaultValue = "1") long page,
                                                   @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(commentService.list(contentId, page, size, UserContext.getUserId()));
    }

    @GetMapping("/api/v1/interaction/comment/mine")
    public ApiResponse<PageResult<CommentVO>> mine(@RequestParam(defaultValue = "1") long page,
                                                   @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(commentService.mine(UserContext.requireUserId(), page, size));
    }
}
