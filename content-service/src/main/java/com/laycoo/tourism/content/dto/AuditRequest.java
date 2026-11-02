package com.laycoo.tourism.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AuditRequest {

    @NotNull(message = "请指定内容")
    private Long id;

    /** APPROVE / REJECT */
    @NotBlank(message = "请指定审核动作")
    private String action;

    private String remark;
}
