package com.tlcsdm.ecovault.service;

import com.tlcsdm.ecovault.dto.RoleMatrixResponse;
import com.tlcsdm.ecovault.entity.Role;
import com.tlcsdm.ecovault.entity.User;

import java.util.List;
import java.util.Set;

/**
 * 角色-页面权限服务 (RBAC 页面级授权)。
 *
 * @author unknowIfGuestInDream
 */
public interface RolePermissionService {

	/**
	 * 初始化各角色的默认页面权限 (仅当角色尚无权限记录时)。
	 */
	void initDefaults();

	/**
	 * 获取角色管理矩阵 (可配置页面 + 各角色权限)。
	 * @return 矩阵响应
	 */
	RoleMatrixResponse getMatrix();

	/**
	 * 更新指定角色的可配置页面权限。
	 * @param role 角色
	 * @param pageKeys 授权的页面 key 列表
	 */
	void updatePermissions(Role role, List<String> pageKeys);

	/**
	 * 获取当前用户可访问的全部页面 key (用于前端菜单渲染)。
	 * @param user 用户
	 * @return 可访问页面 key 集合
	 */
	Set<String> accessiblePageKeys(User user);

	/**
	 * 判断用户是否可访问指定路径对应的页面。
	 * @param user 用户
	 * @param path 页面路径
	 * @return 是否可访问
	 */
	boolean canAccessPath(User user, String path);

}
