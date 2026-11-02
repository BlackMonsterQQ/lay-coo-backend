package com.laycoo.tourism.interaction.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentVO {

    private Long id;
    private Long userId;
    private String nickname;
    private String avatar;
    private Long contentId;
    private String contentTitle;
    private Long parentId;
    private String content;
    private LocalDateTime createTime;
    private boolean mine;
    private boolean deletable;
}
