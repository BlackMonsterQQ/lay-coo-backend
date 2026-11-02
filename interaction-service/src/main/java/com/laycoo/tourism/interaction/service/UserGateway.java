package com.laycoo.tourism.interaction.service;

import com.laycoo.tourism.common.api.ApiResponse;
import com.laycoo.tourism.common.dto.UserBrief;
import com.laycoo.tourism.interaction.feign.UserClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserGateway {

    private final UserClient userClient;

    public Map<Long, UserBrief> batch(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        String joined = ids.stream().distinct().map(String::valueOf).collect(Collectors.joining(","));
        try {
            ApiResponse<List<UserBrief>> response = userClient.batch(joined);
            if (response != null && response.getCode() == 0 && response.getData() != null) {
                Map<Long, UserBrief> map = new HashMap<>();
                for (UserBrief brief : response.getData()) {
                    map.put(brief.getId(), brief);
                }
                return map;
            }
        } catch (Exception e) {
            log.warn("调用用户服务失败: {}", e.getMessage());
        }
        return Map.of();
    }
}
