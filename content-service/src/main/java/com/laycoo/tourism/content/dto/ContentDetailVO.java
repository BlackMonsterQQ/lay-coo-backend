package com.laycoo.tourism.content.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ContentDetailVO {

    private Long id;
    private String title;
    private String summary;
    private Long categoryId;
    private String categoryName;
    private String categoryCode;
    private String categoryType;
    private String categoryIcon;
    private String area;
    private String address;
    private String body;
    private List<String> highlights = new ArrayList<>();
    private String tips;
    private String openingHours;
    private String priceRange;
    private String coverDesc;
    private String source;
    private String status;
    private String auditRemark;
    private Long aiTaskId;
    private Long viewCount;
    private Long favoriteCount;
    private Long likeCount;
    private Long commentCount;
    private BigDecimal rating;
    private Long ratingCount;
    private LocalDateTime publishTime;
    private LocalDateTime createTime;
    private List<String> tags = new ArrayList<>();
}
