package com.laycoo.tourism.ai.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 结构化输出 / 提交内容服务入库的载体。
 */
@Data
public class AiContent {

    private String title;
    private String summary;
    private String categoryCode;
    private String area;
    private String address;
    private String body;
    private List<String> highlights = new ArrayList<>();
    private String tips;
    private String openingHours;
    private String priceRange;
    private String coverDesc;
    private List<String> tags = new ArrayList<>();
    private Long aiTaskId;
}
