package com.laycoo.tourism.ai.feign;

import com.laycoo.tourism.ai.dto.AiContent;
import com.laycoo.tourism.common.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "content-service", url = "${services.content-service-url:http://localhost:8082}")
public interface ContentClient {

    @PostMapping("/internal/content")
    ApiResponse<Long> create(@RequestBody AiContent content);
}
