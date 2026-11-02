package com.laycoo.tourism.content.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class InternalCreateRequest extends ContentSaveRequest {

    @NotNull(message = "缺少 AI 任务标识")
    private Long aiTaskId;
}
