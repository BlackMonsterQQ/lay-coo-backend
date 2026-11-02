package com.laycoo.tourism.gateway;

import com.laycoo.tourism.common.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(scanBasePackages = "com.laycoo.tourism.gateway")
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    @Bean
    public JwtUtil jwtUtil(
            @Value("${security.jwt.secret:laycoo-tourism-local-secret-key-please-change-in-production}") String secret,
            @Value("${security.jwt.expire-hours:168}") long expireHours) {
        return new JwtUtil(secret, expireHours);
    }
}
