package com.tlcsdm.ecovault.dto;

/**
 * 登录响应。
 *
 * @param token    JWT 令牌
 * @param username 用户名
 * @param nickname 昵称
 * @param role     角色
 * @author 梦里不知身是客
 */
public record LoginResponse(
        String token,
        String username,
        String nickname,
        String role
) {
}
