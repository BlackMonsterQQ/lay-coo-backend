package com.laycoo.tourism.ai.plan.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class PlanGenerateRequest {

    @NotBlank(message = "请选择目的地城市")
    private String cityCode;

    @NotNull(message = "请选择开始日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @NotNull(message = "请选择结束日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @Size(max = 5, message = "最多选择 5 个偏好")
    private List<String> preferences = new ArrayList<>();

    @Size(max = 100, message = "补充说明最多 100 个字符")
    private String extraNote;
}
