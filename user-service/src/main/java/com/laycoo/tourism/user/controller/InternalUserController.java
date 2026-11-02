package com.laycoo.tourism.user.controller;

import com.laycoo.tourism.common.api.ApiResponse;
import com.laycoo.tourism.common.dto.UserBrief;
import com.laycoo.tourism.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * 仅供服务间调用（网关不暴露 /internal 前缀）。
 */
@RestController
@RequestMapping("/internal/user")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public ApiResponse<UserBrief> get(@PathVariable Long id) {
        return ApiResponse.ok(userService.internalGet(id));
    }

    @GetMapping("/batch")
    public ApiResponse<List<UserBrief>> batch(@RequestParam String ids) {
        List<Long> idList = Arrays.stream(ids.split(","))
                .filter(s -> !s.isBlank())
                .map(Long::valueOf)
                .toList();
        return ApiResponse.ok(userService.internalBatch(idList));
    }
}
