package com.laycoo.tourism.content.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("category")
public class Category {

    @TableId
    private Long id;

    private String name;

    private String code;

    private String type;

    private String icon;

    private Integer sort;

    private String description;
}
