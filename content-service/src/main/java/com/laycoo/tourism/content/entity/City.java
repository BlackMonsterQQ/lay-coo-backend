package com.laycoo.tourism.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("city")
public class City {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;

    private String name;

    private String province;

    private String country;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private Integer hasContent;

    private Integer enabled;

    private Integer sort;
}
