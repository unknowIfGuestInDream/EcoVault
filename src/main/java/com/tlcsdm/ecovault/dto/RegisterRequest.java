package com.tlcsdm.ecovault.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 用户注册请求。
 *
 * @param username 用户名
 * @param password 明文密码 (服务端使用 BCrypt 加密后存储)
 * @param nickname 昵称
 * @param email    邮箱
 * @author 梦里不知身是客
 */
public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 32, message = "用户名长度需在 3-32 之间")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 64, message = "密码长度需在 6-64 之间")
        String password,

        @Size(max = 64, message = "昵称过长")
        String nickname,

        @Size(max = 128, message = "邮箱过长")
        String email
) {
}
