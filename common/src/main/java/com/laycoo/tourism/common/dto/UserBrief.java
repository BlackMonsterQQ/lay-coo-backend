package com.laycoo.tourism.common.dto;

import lombok.Data;

/**
 * 用户简要信息，用于评论等场景的展示信息补全。
 */
@Data
public class UserBrief {

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String role;
}
