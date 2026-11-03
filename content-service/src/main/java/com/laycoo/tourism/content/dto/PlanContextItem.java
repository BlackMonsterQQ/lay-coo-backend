package com.laycoo.tourism.content.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 提供给 ai-service 的行程上下文，只包含生成行程所需的精简字段。
 */
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
