package com.tlcsdm.ecovault.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 用户实体。
 *
 * <p>
 * 存储系统用户的基本信息、加密后的密码 (BCrypt) 以及角色。
 * </p>
 *
 * @author unknowIfGuestInDream
 * @since 1.0.0
 */
@Entity
@Table(name = "users", indexes = { @Index(name = "idx_users_username", columnList = "username", unique = true) })
/**
 * 用户实体，表示系统登录账户及其基础资料。 保存用户名、BCrypt 密码、角色与启用状态等核心信息。
 *
 * @author unknowIfGuestInDream
 * @since 1.0.0
 */
public class User {

	/** 主键 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 登录用户名 (唯一) */
	@Column(nullable = false, unique = true, length = 64)
	private String username;

	/** BCrypt 加密后的密码 */
	@Column(nullable = false)
	private String password;

	/** 昵称 */
	@Column(length = 64)
	private String nickname;

	/** 邮箱 */
	@Column(length = 128)
	private String email;

	/** 角色 (RBAC) */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private Role role = Role.USER;

	/** 账户是否启用 (管理员可禁用) */
	@Column(nullable = false)
	private boolean enabled = true;

	/** 创建时间 */
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	/** 更新时间 */
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	/** 持久化前填充时间戳 */
	@PrePersist
	public void prePersist() {
		LocalDateTime now = LocalDateTime.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	/** 更新前刷新时间戳 */
	@PreUpdate
	public void preUpdate() {
		this.updatedAt = LocalDateTime.now();
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
	 * @return 用户名。
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * 设置相关属性值。
	 * @param username 用户名。
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * 获取相关属性值。
	 * @return 密码字段内容。
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * 设置相关属性值。
	 * @param password 密码。
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * 获取相关属性值。
	 * @return 昵称。
	 */
	public String getNickname() {
		return nickname;
	}

	/**
	 * 设置相关属性值。
	 * @param nickname nickname参数。
	 */
	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	/**
	 * 获取相关属性值。
	 * @return 邮箱地址。
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * 设置相关属性值。
	 * @param email email参数。
	 */
	public void setEmail(String email) {
		this.email = email;
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
	 * 判断相关状态。
	 * @return 是否启用。
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * 设置相关属性值。
	 * @param enabled 是否启用。
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * 获取相关属性值。
	 * @return 创建时间。
	 */
	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	/**
	 * 设置相关属性值。
	 * @param createdAt createdAt参数。
	 */
	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	/**
	 * 获取相关属性值。
	 * @return 更新时间。
	 */
	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	/**
	 * 设置相关属性值。
	 * @param updatedAt updatedAt参数。
	 */
	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

}
