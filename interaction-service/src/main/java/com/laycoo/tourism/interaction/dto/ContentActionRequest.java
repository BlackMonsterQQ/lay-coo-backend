package com.laycoo.tourism.interaction.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ContentActionRequest {

    @NotNull(message = "请指定内容")
    private Long contentId;
}
