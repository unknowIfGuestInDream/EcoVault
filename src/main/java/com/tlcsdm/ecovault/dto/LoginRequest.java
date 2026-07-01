package com.tlcsdm.ecovault.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 用户登录请求。
 *
 * @param username 用户名
 * @param password 明文密码
 * @author unknowIfGuestInDream
 */
public record LoginRequest(
        @NotBlank(message = "用户名不能为空")
        String username,

        @NotBlank(message = "密码不能为空")
        String password
) {
}
