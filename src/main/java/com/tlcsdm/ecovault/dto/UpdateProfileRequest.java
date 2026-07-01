package com.tlcsdm.ecovault.dto;

import jakarta.validation.constraints.Size;

/**
 * 修改个人信息请求。
 *
 * @param nickname 昵称
 * @param email    邮箱
 * @author unknowIfGuestInDream
 */
public record UpdateProfileRequest(
        @Size(max = 64, message = "昵称过长")
        String nickname,

        @Size(max = 128, message = "邮箱过长")
        String email
) {
}
