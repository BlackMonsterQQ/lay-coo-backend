package com.laycoo.tourism.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("content")
public class Content {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String summary;

    private Long categoryId;

    private String area;

    private String cityCode;

    private String address;

    private String body;

    /** JSON 数组字符串 */
    private String highlights;

    private String tips;

    private String openingHours;

    private String priceRange;

    private String coverDesc;

    private String source;

    /** PENDING / PUBLISHED / REJECTED / DRAFT */
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

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
