package com.laycoo.tourism.interaction.feign;

import com.laycoo.tourism.common.api.ApiResponse;
import com.laycoo.tourism.common.dto.UserBrief;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "user-service", url = "${services.user-service-url:http://localhost:8081}")
public interface UserClient {

    @GetMapping("/internal/user/{id}")
    ApiResponse<UserBrief> get(@PathVariable("id") Long id);

    @GetMapping("/internal/user/batch")
    ApiResponse<List<UserBrief>> batch(@RequestParam("ids") String ids);
}
