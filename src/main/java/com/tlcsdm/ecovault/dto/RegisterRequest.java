package com.tlcsdm.ecovault.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 用户注册请求。
 *
 * @param username 用户名
 * @param password 明文密码 (服务端使用 BCrypt 加密后存储)
 * @param nickname 昵称
 * @param email 邮箱
 * @param role 角色 (可空，默认为 USER；仅管理员创建用户时可指定)
 * @author unknowIfGuestInDream
 */
public record RegisterRequest(
		@NotBlank(message = "用户名不能为空") @Size(min = 3, max = 32, message = "用户名长度需在 3-32 之间") String username,

		@NotBlank(message = "密码不能为空") @Size(min = 6, max = 64, message = "密码长度需在 6-64 之间") String password,

		@Size(max = 64, message = "昵称过长") String nickname,

		@Size(max = 128, message = "邮箱过长") String email,

		String role) {

	/**
	 * 兼容旧调用的构造方法，默认角色为空 (由服务层落为 USER)。
	 * @param username 用户名
	 * @param password 明文密码
	 * @param nickname 昵称
	 * @param email 邮箱
	 */
	public RegisterRequest(String username, String password, String nickname, String email) {
		this(username, password, nickname, email, null);
	}
}
