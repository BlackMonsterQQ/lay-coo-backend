package com.laycoo.tourism.common.security;

/**
 * 登录用户身份。
 */
public record UserPrincipal(Long id, String username, String role) {

    public boolean admin() {
        return "ADMIN".equalsIgnoreCase(role);
    }
}
