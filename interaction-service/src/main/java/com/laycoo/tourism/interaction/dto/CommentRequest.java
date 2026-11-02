package com.laycoo.tourism.interaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentRequest {

    @NotNull(message = "请指定内容")
    private Long contentId;

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 500, message = "评论最多 500 个字符")
    private String content;

    private Long parentId;
}
