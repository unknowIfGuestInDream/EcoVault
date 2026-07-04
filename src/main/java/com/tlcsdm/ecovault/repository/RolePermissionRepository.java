package com.tlcsdm.ecovault.repository;

import com.tlcsdm.ecovault.entity.Role;
import com.tlcsdm.ecovault.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 角色-页面权限数据访问接口。
 *
 * @author unknowIfGuestInDream
 */
public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

	/**
	 * 查询指定角色的全部页面权限。
	 * @param role 角色
	 * @return 权限列表
	 */
	List<RolePermission> findByRole(Role role);

	/**
	 * 删除指定角色的全部页面权限。
	 * @param role 角色
	 */
	void deleteByRole(Role role);

	/**
	 * 判断指定角色是否已存在任意权限记录。
	 * @param role 角色
	 * @return 是否存在
	 */
	boolean existsByRole(Role role);

}
