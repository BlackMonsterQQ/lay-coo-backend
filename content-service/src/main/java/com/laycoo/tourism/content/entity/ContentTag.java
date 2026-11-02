package com.laycoo.tourism.content.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("content_tag")
public class ContentTag {

    private Long contentId;

    private Long tagId;
}
