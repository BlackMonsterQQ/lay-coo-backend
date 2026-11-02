package com.laycoo.tourism.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateTaskRequest {

    @NotBlank(message = "请指定内容分类")
    private String categoryCode;

    @Size(max = 64, message = "区域名称过长")
    private String area;

    @Min(value = 1, message = "至少生成 1 条")
    @Max(value = 5, message = "单次最多生成 5 条")
    private int count = 2;

    @Size(max = 200, message = "关键词最多 200 个字符")
    private String keywords;
}
