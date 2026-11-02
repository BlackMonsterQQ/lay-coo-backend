package com.laycoo.tourism.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_task")
public class AiTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String categoryCode;

    private String area;

    private Integer targetCount;

    private String keywords;

    private String status;

    private String model;

    private String prompt;

    private String result;

    private String error;

    private Long costTokens;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
