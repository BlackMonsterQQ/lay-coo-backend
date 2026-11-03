package com.laycoo.tourism.content.dto;

import lombok.Data;

@Data
public class ContentQuery {

    private Long categoryId;

    private String categoryCode;

    private String type;

    private String area;

    private String cityCode;

    private String tag;

    private String keyword;

    /** hot / new */
    private String sort = "hot";

    private String status;

    private String source;

    private long page = 1;

    private long size = 12;
}
