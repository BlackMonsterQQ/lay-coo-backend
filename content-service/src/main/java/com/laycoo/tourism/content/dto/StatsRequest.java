package com.laycoo.tourism.content.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StatsRequest {

    /** view / favorite / like / comment / rating */
    private String type;

    private long delta = 1;

    private BigDecimal ratingAvg;

    private Long ratingCount;
}
