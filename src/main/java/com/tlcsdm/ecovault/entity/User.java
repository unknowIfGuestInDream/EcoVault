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
 */
@Entity
@Table(name = "users", indexes = { @Index(name = "idx_users_username", columnList = "username", unique = true) })
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

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

}
