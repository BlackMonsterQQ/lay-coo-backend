package com.laycoo.tourism.common.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 内容卡片视图，供内容服务与推荐/互动服务之间传输。
 */
@Data
public class ContentCard {

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
    private String coverDesc;
    private List<String> tags = new ArrayList<>();
    private String source;
    private String status;
    private Long viewCount;
    private Long favoriteCount;
    private Long likeCount;
    private Long commentCount;
    private BigDecimal rating;
    private Long ratingCount;
    private LocalDateTime publishTime;
    private LocalDateTime createTime;
}
