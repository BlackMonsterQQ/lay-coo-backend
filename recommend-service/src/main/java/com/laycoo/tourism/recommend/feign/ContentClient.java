package com.laycoo.tourism.recommend.feign;

import com.laycoo.tourism.common.api.ApiResponse;
import com.laycoo.tourism.common.dto.ContentCard;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "content-service", url = "${services.content-service-url:http://localhost:8082}")
public interface ContentClient {

    @GetMapping("/internal/content/candidates")
    ApiResponse<List<ContentCard>> candidates(@RequestParam("limit") int limit);
}
