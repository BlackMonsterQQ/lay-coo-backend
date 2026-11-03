package com.laycoo.tourism.ai.plan.service;

import com.laycoo.tourism.ai.config.AiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 本地演示用内存限流。多实例部署时应替换为 Redis 计数器。
 */
@Component
@RequiredArgsConstructor
public class PlanRateLimiter {

    private final AiProperties properties;
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();

    public boolean allow(Long userId, String ip) {
        String day = LocalDate.now().toString();
        String key = (userId == null ? "ip:" + ip : "u:" + userId) + ":" + day;
        int limit = userId == null ? properties.getGuestDailyLimit() : properties.getUserDailyLimit();
        Counter counter = counters.computeIfAbsent(key, k -> new Counter());
        synchronized (counter) {
            if (!day.equals(counter.day)) {
                counter.day = day;
                counter.count = 0;
            }
            if (counter.count >= limit) {
                return false;
            }
            counter.count++;
            return true;
        }
    }

    private static class Counter {
        private String day = "";
        private int count;
    }
}
