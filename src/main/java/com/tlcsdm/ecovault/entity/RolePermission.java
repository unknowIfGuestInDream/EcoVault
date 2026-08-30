package com.tlcsdm.ecovault.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 角色-页面权限实体 (RBAC 页面级授权)。
 *
 * <p>
 * 每条记录表示某个角色被授予访问某个可配置页面的权限。管理员可在角色管理中为角色分配这些权限。
 * </p>
 *
 * @author unknowIfGuestInDream
 */
@Entity
@Table(name = "role_permissions",
		uniqueConstraints = @UniqueConstraint(name = "uk_role_page", columnNames = { "role", "page_key" }))
/**
 * 角色权限实体，表示角色与页面访问权限的关联关系。 用于控制不同角色可访问的功能页面范围。
 *
 * @author unknowIfGuestInDream
 * @since 1.0.0
 */
public class RolePermission {

	/** 主键 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 角色 */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private Role role;

	/** 页面 key (对应 {@link MenuPage#getKey()}) */
	@Column(name = "page_key", nullable = false, length = 32)
	private String pageKey;

	/**
	 * 构造RolePermission实例并注入所需依赖。
	 */
	public RolePermission() {
	}

	/**
	 * 构造RolePermission实例并注入所需依赖。
	 * @param role 角色标识。
	 * @param pageKey pageKey参数。
	 */
	public RolePermission(Role role, String pageKey) {
		this.role = role;
		this.pageKey = pageKey;
	}

	/**
	 * 获取相关属性值。
	 * @return 主键编号。
	 */
	public Long getId() {
		return id;
	}

	/**
	 * 设置相关属性值。
	 * @param id 主键或记录编号。
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * 获取相关属性值。
	 * @return 角色信息。
	 */
	public Role getRole() {
		return role;
	}

	/**
	 * 设置相关属性值。
	 * @param role 角色标识。
	 */
	public void setRole(Role role) {
		this.role = role;
	}

	/**
	 * 获取相关属性值。
	 * @return 方法执行结果。
	 */
	public String getPageKey() {
		return pageKey;
	}

	/**
	 * 设置相关属性值。
	 * @param pageKey pageKey参数。
	 */
	public void setPageKey(String pageKey) {
		this.pageKey = pageKey;
	}

}
