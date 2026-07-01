package com.tlcsdm.ecovault.dto;

/**
 * 登录响应。
 *
 * @param token JWT 令牌
 * @param username 用户名
 * @param nickname 昵称
 * @param role 角色
 * @author unknowIfGuestInDream
 */
public record LoginResponse(String token, String username, String nickname, String role) {
}
