package com.laycoo.tourism.recommend.feign;

import com.laycoo.tourism.common.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "interaction-service", url = "${services.interaction-service-url:http://localhost:8085}")
public interface InteractionClient {

    @GetMapping("/internal/interaction/profile")
    ApiResponse<Map<String, Object>> profile(@RequestParam("userId") Long userId);
}
