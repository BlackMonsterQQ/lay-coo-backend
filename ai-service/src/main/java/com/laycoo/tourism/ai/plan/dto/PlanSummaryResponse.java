package com.laycoo.tourism.ai.plan.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PlanSummaryResponse {

    private String planNo;
    private String city;
    private String cityCode;
    private String originCity;
    private String originCityCode;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer dayCount;
    private Integer nights;
    private String status;
    private String title;
    private String summary;
    private String transportPreference;
    private String model;
    private LocalDateTime createTime;
}
