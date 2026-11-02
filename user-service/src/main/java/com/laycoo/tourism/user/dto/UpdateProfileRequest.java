package com.laycoo.tourism.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(max = 20, message = "昵称最多 20 个字符")
    private String nickname;

    @Size(max = 255, message = "头像地址过长")
    private String avatar;
}
