package com.laycoo.tourism.common.security;

import com.laycoo.tourism.common.exception.BusinessException;

/**
 * 当前请求用户上下文，由网关下发的 X-User-* 请求头填充。
 */
public final class UserContext {

    private static final ThreadLocal<UserPrincipal> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(UserPrincipal principal) {
        HOLDER.set(principal);
    }

    public static UserPrincipal get() {
        return HOLDER.get();
    }

    public static Long getUserId() {
        UserPrincipal principal = HOLDER.get();
        return principal == null ? null : principal.id();
    }

    public static String getRole() {
        UserPrincipal principal = HOLDER.get();
        return principal == null ? null : principal.role();
    }

    public static boolean isAdmin() {
        UserPrincipal principal = HOLDER.get();
        return principal != null && principal.admin();
    }

    public static Long requireUserId() {
        Long userId = getUserId();
        if (userId == null) {
            throw BusinessException.unauthorized("请先登录");
        }
        return userId;
    }

    public static void requireAdmin() {
        if (!isAdmin()) {
            throw BusinessException.forbidden("需要管理员权限");
        }
    }

    public static void clear() {
        HOLDER.remove();
    }
}
