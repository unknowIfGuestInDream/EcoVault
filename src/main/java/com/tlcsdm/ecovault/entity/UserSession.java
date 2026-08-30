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
 * @since 1.0.0
 */
@Entity
@Table(name = "user_sessions", indexes = { @Index(name = "idx_sessions_jti", columnList = "jti", unique = true),
		@Index(name = "idx_sessions_user", columnList = "user_id") })
/**
 * 用户会话实体，表示用户一次登录产生的会话记录。 用于控制设备登录数量与会话失效状态。
 *
 * @author unknowIfGuestInDream
 * @since 1.0.0
 */
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

	/**
	 * 在实体首次持久化前初始化时间等字段。
	 */
	@PrePersist
	public void prePersist() {
		LocalDateTime now = LocalDateTime.now();
		this.createdAt = now;
		this.lastActiveAt = now;
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
	 * @return 方法执行结果。
	 */
	public Long getUserId() {
		return userId;
	}

	/**
	 * 设置相关属性值。
	 * @param userId 用户编号。
	 */
	public void setUserId(Long userId) {
		this.userId = userId;
	}

	/**
	 * 获取相关属性值。
	 * @return 方法执行结果。
	 */
	public String getJti() {
		return jti;
	}

	/**
	 * 设置相关属性值。
	 * @param jti 会话唯一标识。
	 */
	public void setJti(String jti) {
		this.jti = jti;
	}

	/**
	 * 获取相关属性值。
	 * @return 方法执行结果。
	 */
	public String getDeviceInfo() {
		return deviceInfo;
	}

	/**
	 * 设置相关属性值。
	 * @param deviceInfo 设备信息。
	 */
	public void setDeviceInfo(String deviceInfo) {
		this.deviceInfo = deviceInfo;
	}

	/**
	 * 获取相关属性值。
	 * @return 方法执行结果。
	 */
	public String getIp() {
		return ip;
	}

	/**
	 * 设置相关属性值。
	 * @param ip 客户端 IP 地址。
	 */
	public void setIp(String ip) {
		this.ip = ip;
	}

	/**
	 * 判断相关状态。
	 * @return 方法执行结果。
	 */
	public boolean isActive() {
		return active;
	}

	/**
	 * 设置相关属性值。
	 * @param active active参数。
	 */
	public void setActive(boolean active) {
		this.active = active;
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
	 * @return 方法执行结果。
	 */
	public LocalDateTime getLastActiveAt() {
		return lastActiveAt;
	}

	/**
	 * 设置相关属性值。
	 * @param lastActiveAt lastActiveAt参数。
	 */
	public void setLastActiveAt(LocalDateTime lastActiveAt) {
		this.lastActiveAt = lastActiveAt;
	}

}
