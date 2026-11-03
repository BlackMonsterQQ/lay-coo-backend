package com.laycoo.tourism.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ContentSaveRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 128, message = "标题最多 128 个字符")
    private String title;

    @Size(max = 512, message = "摘要最多 512 个字符")
    private String summary;

    @NotBlank(message = "请选择分类")
    private String categoryCode;

    private String area;

    private String cityCode = "hangzhou";

    private String address;

    @NotBlank(message = "正文不能为空")
    private String body;

    private List<String> highlights = new ArrayList<>();

    private String tips;

    private String openingHours;

    private String priceRange;

    private String coverDesc;

    private List<String> tags = new ArrayList<>();
}
