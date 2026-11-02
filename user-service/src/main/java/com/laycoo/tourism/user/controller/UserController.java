package com.laycoo.tourism.user.controller;

import com.laycoo.tourism.common.api.ApiResponse;
import com.laycoo.tourism.common.security.UserContext;
import com.laycoo.tourism.user.dto.AuthResponse;
import com.laycoo.tourism.user.dto.LoginRequest;
import com.laycoo.tourism.user.dto.RegisterRequest;
import com.laycoo.tourism.user.dto.UpdateProfileRequest;
import com.laycoo.tourism.user.dto.UserVO;
import com.laycoo.tourism.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(userService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(userService.login(request));
    }

    @GetMapping("/info")
    public ApiResponse<UserVO> info() {
        return ApiResponse.ok(userService.info(UserContext.requireUserId()));
    }

    @PutMapping("/info")
    public ApiResponse<UserVO> update(@Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.ok(userService.updateProfile(UserContext.requireUserId(), request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        // 无状态 JWT，前端清除本地令牌即可；预留用于后续 Redis 令牌黑名单
        return ApiResponse.ok();
    }
}
