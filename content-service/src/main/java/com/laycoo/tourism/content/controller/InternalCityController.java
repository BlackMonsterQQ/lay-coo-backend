package com.laycoo.tourism.content.controller;

import com.laycoo.tourism.common.api.ApiResponse;
import com.laycoo.tourism.content.entity.City;
import com.laycoo.tourism.content.service.CityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 服务间内部城市接口，网关不暴露 /internal 前缀。
 */
@RestController
@RequestMapping("/internal/city")
@RequiredArgsConstructor
public class InternalCityController {

    private final CityService cityService;

    @GetMapping("/list")
    public ApiResponse<List<City>> list() {
        return ApiResponse.ok(cityService.list());
    }

    @GetMapping("/{code}")
    public ApiResponse<City> get(@PathVariable String code) {
        return ApiResponse.ok(cityService.require(code));
    }
}
