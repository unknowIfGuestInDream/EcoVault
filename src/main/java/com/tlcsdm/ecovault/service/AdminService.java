package com.tlcsdm.ecovault.service;

import com.tlcsdm.ecovault.dto.AdminUserResponse;
import com.tlcsdm.ecovault.dto.UpdateUserRequest;

import java.util.List;

/**
 * 管理后台服务 (仅管理员)。
 *
 * @author unknowIfGuestInDream
 */
public interface AdminService {

	/**
	 * 查询全部用户列表。
	 * @return 用户列表
	 */
	List<AdminUserResponse> listUsers();

	/**
	 * 启用或禁用用户。
	 * @param userId 用户 ID
	 * @param enabled 是否启用
	 */
	void setUserEnabled(Long userId, boolean enabled);

	/**
	 * 更新用户信息 (昵称、邮箱、角色、状态，可选重置密码)。
	 * @param userId 用户 ID
	 * @param request 更新请求
	 * @return 更新后的用户信息
	 */
	AdminUserResponse updateUser(Long userId, UpdateUserRequest request);

	/**
	 * 删除用户。
	 * @param userId 用户 ID
	 */
	void deleteUser(Long userId);

}
