package com.laycoo.tourism.ai.plan.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class PlanContextItem {

    private Long id;
    private String title;
    private String categoryName;
    private String categoryCode;
    private String cityCode;
    private String area;
    private String summary;
    private List<String> tags = new ArrayList<>();
    private String openingHours;
    private String priceRange;
    private BigDecimal rating;
}
