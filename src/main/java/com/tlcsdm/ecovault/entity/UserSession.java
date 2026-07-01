package com.tlcsdm.ecovault.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 用户登录会话实体。
 *
 * <p>
 * 用于实现单设备登录限制：每次登录生成一个会话记录 (含 JWT 的唯一标识 jti)， 当同一用户的活跃会话数超过配置的最大设备数时，最早的会话会被强制失效。 JWT
 * 校验时会检查对应会话是否仍然处于活跃状态。
 * </p>
 *
 * @author unknowIfGuestInDream
 */
@Entity
@Table(name = "user_sessions", indexes = { @Index(name = "idx_sessions_jti", columnList = "jti", unique = true),
		@Index(name = "idx_sessions_user", columnList = "user_id") })
public class UserSession {

	/** 主键 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 所属用户 ID */
	@Column(name = "user_id", nullable = false)
	private Long userId;

	/** JWT 唯一标识 (JWT ID) */
	@Column(nullable = false, unique = true, length = 64)
	private String jti;

	/** 设备信息 (User-Agent 摘要) */
	@Column(name = "device_info", length = 512)
	private String deviceInfo;

	/** 登录 IP */
	@Column(length = 64)
	private String ip;

	/** 会话是否活跃 */
	@Column(nullable = false)
	private boolean active = true;

	/** 创建时间 */
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	/** 最近活跃时间 */
	@Column(name = "last_active_at", nullable = false)
	private LocalDateTime lastActiveAt;

	@PrePersist
	public void prePersist() {
		LocalDateTime now = LocalDateTime.now();
		this.createdAt = now;
		this.lastActiveAt = now;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public String getJti() {
		return jti;
	}

	public void setJti(String jti) {
		this.jti = jti;
	}

	public String getDeviceInfo() {
		return deviceInfo;
	}

	public void setDeviceInfo(String deviceInfo) {
		this.deviceInfo = deviceInfo;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getLastActiveAt() {
		return lastActiveAt;
	}

	public void setLastActiveAt(LocalDateTime lastActiveAt) {
		this.lastActiveAt = lastActiveAt;
	}

}
