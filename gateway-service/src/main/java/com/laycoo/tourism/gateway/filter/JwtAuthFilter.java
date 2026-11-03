package com.laycoo.tourism.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laycoo.tourism.common.api.ApiResponse;
import com.laycoo.tourism.common.security.JwtUtil;
import com.laycoo.tourism.common.security.UserPrincipal;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 网关统一鉴权：校验 JWT，并把用户身份通过请求头透传给下游服务。
 * 对公开接口允许匿名访问，但若携带有效 token 仍会注入用户身份（用于个性化推荐）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements GlobalFilter, Ordered {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USERNAME = "X-User-Name";
    public static final String HEADER_ROLE = "X-User-Role";

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        if (HttpMethod.OPTIONS.equals(request.getMethod())) {
            return chain.filter(exchange);
        }

        // 先清除客户端可能伪造的身份头，只信任网关注入的值
        ServerHttpRequest.Builder builder = request.mutate()
                .headers(headers -> {
                    headers.remove(HEADER_USER_ID);
                    headers.remove(HEADER_USERNAME);
                    headers.remove(HEADER_ROLE);
                });

        UserPrincipal principal = resolveToken(request);
        if (principal != null) {
            builder.header(HEADER_USER_ID, String.valueOf(principal.id()));
            builder.header(HEADER_ROLE, principal.role() == null ? "USER" : principal.role());
            if (StringUtils.hasText(principal.username())) {
                String username = principal.username().replaceAll("[^\\x20-\\x7E]", "");
                if (StringUtils.hasText(username)) {
                    builder.header(HEADER_USERNAME, username);
                }
            }
        }

        if (isAdminPath(path) && (principal == null || !principal.admin())) {
            return writeError(exchange, HttpStatus.FORBIDDEN, 403, "需要管理员权限");
        }
        if (!isPublic(request.getMethod(), path) && principal == null) {
            return writeError(exchange, HttpStatus.UNAUTHORIZED, 401, "请先登录");
        }

        return chain.filter(exchange.mutate().request(builder.build()).build());
    }

    private UserPrincipal resolveToken(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst("Authorization");
        if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
            return null;
        }
        String token = header.substring(7).trim();
        if (token.isEmpty()) {
            return null;
        }
        try {
            Claims claims = jwtUtil.parse(token);
            return new UserPrincipal(Long.valueOf(claims.getSubject()),
                    claims.get("username", String.class),
                    claims.get("role", String.class));
        } catch (Exception e) {
            log.debug("JWT 校验失败: {}", e.getMessage());
            return null;
        }
    }

    private boolean isAdminPath(String path) {
        return path.startsWith("/api/v1/admin/");
    }

    private boolean isPublic(HttpMethod method, String path) {
        if (path.startsWith("/actuator")) {
            return true;
        }
        if (HttpMethod.GET.equals(method)) {
            return path.startsWith("/api/v1/content")
                    || path.startsWith("/api/v1/category")
                    || path.startsWith("/api/v1/tag")
                    || path.startsWith("/api/v1/search")
                    || path.startsWith("/api/v1/recommend")
                    || path.startsWith("/api/v1/city")
                    || path.startsWith("/api/v1/plan")
                    || path.equals("/api/v1/interaction/comment/list")
                    || path.equals("/api/v1/interaction/stats")
                    || path.startsWith("/api/v1/rate/summary")
                    || path.startsWith("/api/v1/rate/list");
        }
        if (HttpMethod.POST.equals(method)) {
            return path.equals("/api/v1/user/login")
                    || path.equals("/api/v1/user/register")
                    || path.startsWith("/api/v1/plan/");
        }
        return false;
    }

    private Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status, int code, String msg) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(ApiResponse.fail(code, msg));
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            byte[] fallback = ("{\"code\":" + code + ",\"msg\":\"" + msg + "\",\"data\":null}")
                    .getBytes(StandardCharsets.UTF_8);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(fallback)));
        }
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
