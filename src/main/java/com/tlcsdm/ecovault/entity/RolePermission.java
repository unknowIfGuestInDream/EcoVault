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

	public RolePermission() {
	}

	public RolePermission(Role role, String pageKey) {
		this.role = role;
		this.pageKey = pageKey;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	public String getPageKey() {
		return pageKey;
	}

	public void setPageKey(String pageKey) {
		this.pageKey = pageKey;
	}

}
