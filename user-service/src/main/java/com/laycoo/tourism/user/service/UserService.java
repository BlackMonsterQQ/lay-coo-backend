package com.laycoo.tourism.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.laycoo.tourism.common.dto.UserBrief;
import com.laycoo.tourism.common.exception.BusinessException;
import com.laycoo.tourism.common.security.JwtUtil;
import com.laycoo.tourism.user.dto.AuthResponse;
import com.laycoo.tourism.user.dto.LoginRequest;
import com.laycoo.tourism.user.dto.RegisterRequest;
import com.laycoo.tourism.user.dto.UpdateProfileRequest;
import com.laycoo.tourism.user.dto.UserVO;
import com.laycoo.tourism.user.entity.SysUser;
import com.laycoo.tourism.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthResponse register(RegisterRequest request) {
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername()));
        if (exists != null && exists > 0) {
            throw new BusinessException("该用户名已被注册");
        }
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(StringUtils.hasText(request.getNickname())
                ? request.getNickname() : request.getUsername());
        user.setRole("USER");
        user.setStatus(1);
        user.setAvatar("");
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        return auth(user);
    }

    public AuthResponse login(LoginRequest request) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername())
                .last("limit 1"));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码不正确");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(403, "账号已被禁用");
        }
        return auth(user);
    }

    private AuthResponse auth(SysUser user) {
        String token = jwtUtil.createToken(user.getId(), user.getUsername(), user.getRole());
        return new AuthResponse(token, toVO(user));
    }

    public UserVO info(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        return toVO(user);
    }

    public UserVO updateProfile(Long userId, UpdateProfileRequest request) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        if (StringUtils.hasText(request.getNickname())) {
            user.setNickname(request.getNickname().trim());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        return toVO(user);
    }

    public UserBrief internalGet(Long id) {
        SysUser user = userMapper.selectById(id);
        return user == null ? null : toBrief(user);
    }

    public List<UserBrief> internalBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<SysUser> users = userMapper.selectBatchIds(ids);
        List<UserBrief> result = new ArrayList<>();
        for (SysUser user : users) {
            result.add(toBrief(user));
        }
        return result;
    }

    private UserVO toVO(SysUser user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setRole(user.getRole());
        vo.setCreateTime(user.getCreateTime() == null ? "" : user.getCreateTime().format(DATE));
        return vo;
    }

    private UserBrief toBrief(SysUser user) {
        UserBrief brief = new UserBrief();
        brief.setId(user.getId());
        brief.setUsername(user.getUsername());
        brief.setNickname(user.getNickname());
        brief.setAvatar(user.getAvatar());
        brief.setRole(user.getRole());
        return brief;
    }
}
