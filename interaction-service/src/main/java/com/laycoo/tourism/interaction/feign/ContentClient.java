package com.laycoo.tourism.interaction.feign;

import com.laycoo.tourism.common.api.ApiResponse;
import com.laycoo.tourism.common.dto.ContentCard;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "content-service", url = "${services.content-service-url:http://localhost:8082}")
public interface ContentClient {

    @PostMapping("/internal/content/batch")
    ApiResponse<List<ContentCard>> batch(@RequestBody List<Long> ids);

    @PostMapping("/internal/content/{id}/stats")
    ApiResponse<Void> stats(@PathVariable("id") Long id, @RequestBody StatsPayload payload);
}
