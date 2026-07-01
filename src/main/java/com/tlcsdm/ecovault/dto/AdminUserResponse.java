package com.tlcsdm.ecovault.dto;

import java.time.LocalDateTime;

/**
 * 管理后台用户信息响应。
 *
 * @param id 用户 ID
 * @param username 用户名
 * @param nickname 昵称
 * @param email 邮箱
 * @param role 角色
 * @param enabled 是否启用
 * @param createdAt 注册时间
 * @author unknowIfGuestInDream
 */
public record AdminUserResponse(Long id, String username, String nickname, String email, String role, boolean enabled,
		LocalDateTime createdAt) {
}
