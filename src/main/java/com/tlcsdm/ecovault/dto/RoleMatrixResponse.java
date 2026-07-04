package com.tlcsdm.ecovault.dto;

import java.util.List;

/**
 * 角色管理矩阵响应。
 *
 * @param pages 可配置页面列表 (可分配)
 * @param roles 各角色当前拥有的页面权限
 * @author unknowIfGuestInDream
 */
public record RoleMatrixResponse(List<PageInfo> pages, List<RolePermissionView> roles) {

	/**
	 * 页面信息。
	 *
	 * @param key 页面 key
	 * @param label 页面名称
	 * @param group 所属分组
	 */
	public record PageInfo(String key, String label, String group) {
	}

	/**
	 * 角色的页面权限视图。
	 *
	 * @param role 角色
	 * @param allowedPages 已授权的可配置页面 key 列表
	 * @author unknowIfGuestInDream
	 */
	public record RolePermissionView(String role, List<String> allowedPages) {
	}

}
