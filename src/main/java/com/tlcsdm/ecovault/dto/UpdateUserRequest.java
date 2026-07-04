package com.tlcsdm.ecovault.dto;

import jakarta.validation.constraints.Size;

/**
 * 管理后台更新用户请求。
 *
 * @param nickname 昵称
 * @param email 邮箱
 * @param role 角色 (USER/ADMIN)
 * @param enabled 是否启用
 * @param password 重置后的新密码 (可空，为空则不修改)
 * @author unknowIfGuestInDream
 */
public record UpdateUserRequest(@Size(max = 64, message = "昵称过长") String nickname,

		@Size(max = 128, message = "邮箱过长") String email,

		String role,

		Boolean enabled,

		@Size(max = 64, message = "密码过长") String password) {
}
