package com.laycoo.tourism.ai.feign;

import com.laycoo.tourism.ai.dto.AiContent;
import com.laycoo.tourism.ai.plan.dto.CityInfo;
import com.laycoo.tourism.ai.plan.dto.PlanContextItem;
import com.laycoo.tourism.common.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "content-service", url = "${services.content-service-url:http://localhost:8082}")
public interface ContentClient {

    @PostMapping("/internal/content")
    ApiResponse<Long> create(@RequestBody AiContent content);

    @GetMapping("/internal/city/{code}")
    ApiResponse<CityInfo> city(@PathVariable("code") String code);

    @GetMapping("/internal/content/plan-context")
    ApiResponse<List<PlanContextItem>> planContext(
            @RequestParam("cityCode") String cityCode,
            @RequestParam(value = "preferences", required = false) String preferences,
            @RequestParam("limit") int limit);
}
