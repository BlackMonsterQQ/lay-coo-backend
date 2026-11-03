package com.laycoo.tourism.ai.plan.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PlanDetailResponse {

    private String planNo;
    private String status;
    private String city;
    private String cityCode;
    private String originCity;
    private String originCityCode;
    private String transportPreference;
    private LocalDate startDate;
    private LocalDate endDate;
    private int dayCount;
    private int nights;
    private List<String> preferences;
    private String title;
    private String summary;
    private JsonNode plan;
    private String error;
    private int progress;
    private String model;
    private LocalDateTime createTime;
}
