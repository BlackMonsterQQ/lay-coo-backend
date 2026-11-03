package com.laycoo.tourism.ai.plan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("travel_plan")
public class TravelPlan {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String planNo;

    private Long userId;

    private String cityCode;

    private String cityName;

    private String originCityCode;

    private String originCityName;

    private String transportPreference;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer dayCount;

    private String preferences;

    private String extraNote;

    private String requestHash;

    /** PENDING / RUNNING / SUCCESS / FAILED */
    private String status;

    private String title;

    private String summary;

    private String contentJson;

    private String model;

    private String prompt;

    private String error;

    private Integer progress;

    private Long costTokens;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
