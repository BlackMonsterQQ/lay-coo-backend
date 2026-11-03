package com.laycoo.tourism.content.controller;

import com.laycoo.tourism.common.api.ApiResponse;
import com.laycoo.tourism.content.entity.City;
import com.laycoo.tourism.content.service.CityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/city")
@RequiredArgsConstructor
public class CityController {

    private final CityService cityService;

    @GetMapping("/list")
    public ApiResponse<List<City>> list() {
        return ApiResponse.ok(cityService.list());
    }
}
