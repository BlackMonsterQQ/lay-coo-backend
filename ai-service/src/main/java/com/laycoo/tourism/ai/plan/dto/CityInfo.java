package com.laycoo.tourism.ai.plan.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CityInfo {

    private String code;
    private String name;
    private String country;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer hasContent;
}
