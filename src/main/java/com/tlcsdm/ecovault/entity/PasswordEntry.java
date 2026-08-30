package com.tlcsdm.ecovault.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 密码条目实体。
 *
 * <p>
 * 用于存储用户的账号密码信息。其中 {@code secret} (密码本身) 与 {@code notes} 在落库前使用 AES 加密，读取时解密，确保数据库中不保存明文。
 * {@code tags} 用于标签筛选，同样以加密形式存储，避免包含敏感关键字时被 Web 防火墙拦截。
 * </p>
 *
 * @author unknowIfGuestInDream
 * @since 1.0.0
 */
@Entity
@Table(name = "password_entries", indexes = { @Index(name = "idx_pwd_user", columnList = "user_id"),
		@Index(name = "idx_pwd_category", columnList = "category") })
/**
 * 密码条目实体，表示用户保存在保险箱中的一项凭据。 包含站点、账号、加密后的密码及相关备注信息。
 *
 * @author unknowIfGuestInDream
 * @since 1.0.0
 */
public class PasswordEntry {

	/** 主键 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 所属用户 ID */
	@Column(name = "user_id", nullable = false)
	private Long userId;

	/** 条目标题 (如：GitHub 账号) */
	@Column(nullable = false, length = 128)
	private String title;

	/** 登录用户名/账号 */
	@Column(length = 128)
	private String account;

	/** 密码密文 (AES 加密) */
	@Lob
	@Column(nullable = false)
	private String secret;

	/** 站点地址 */
	@Column(length = 256)
	private String url;

	/** 备注密文 (AES 加密) */
	@Lob
	private String notes;

	/** 分类 */
	@Column(length = 64)
	private String category;

	/** 标签密文 (AES 加密，明文为英文逗号分隔) */
	@Column(length = 512)
	private String tags;

	/** 密码强度评分 (0-100) */
	@Column(name = "strength_score")
	private int strengthScore;

	/** 密码强度等级 (WEAK/MEDIUM/STRONG) */
	@Column(name = "strength_level", length = 16)
	private String strengthLevel;

	/** 创建时间 */
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	/** 更新时间 */
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	/**
	 * 在实体首次持久化前初始化时间等字段。
	 */
	@PrePersist
	public void prePersist() {
		LocalDateTime now = LocalDateTime.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	/**
	 * 在实体更新前刷新时间等字段。
	 */
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
	public String getTitle() {
		return title;
	}

	/**
	 * 设置相关属性值。
	 * @param title title参数。
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * 获取相关属性值。
	 * @return 方法执行结果。
	 */
	public String getAccount() {
		return account;
	}

	/**
	 * 设置相关属性值。
	 * @param account account参数。
	 */
	public void setAccount(String account) {
		this.account = account;
	}

	/**
	 * 获取相关属性值。
	 * @return 方法执行结果。
	 */
	public String getSecret() {
		return secret;
	}

	/**
	 * 设置相关属性值。
	 * @param secret JWT 签名密钥。
	 */
	public void setSecret(String secret) {
		this.secret = secret;
	}

	/**
	 * 获取相关属性值。
	 * @return 方法执行结果。
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * 设置相关属性值。
	 * @param url url参数。
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * 获取相关属性值。
	 * @return 方法执行结果。
	 */
	public String getNotes() {
		return notes;
	}

	/**
	 * 设置相关属性值。
	 * @param notes notes参数。
	 */
	public void setNotes(String notes) {
		this.notes = notes;
	}

	/**
	 * 获取相关属性值。
	 * @return 方法执行结果。
	 */
	public String getCategory() {
		return category;
	}

	/**
	 * 设置相关属性值。
	 * @param category category参数。
	 */
	public void setCategory(String category) {
		this.category = category;
	}

	/**
	 * 获取相关属性值。
	 * @return 方法执行结果。
	 */
	public String getTags() {
		return tags;
	}

	/**
	 * 设置相关属性值。
	 * @param tags tags参数。
	 */
	public void setTags(String tags) {
		this.tags = tags;
	}

	/**
	 * 获取相关属性值。
	 * @return 方法执行结果。
	 */
	public int getStrengthScore() {
		return strengthScore;
	}

	/**
	 * 设置相关属性值。
	 * @param strengthScore strengthScore参数。
	 */
	public void setStrengthScore(int strengthScore) {
		this.strengthScore = strengthScore;
	}

	/**
	 * 获取相关属性值。
	 * @return 方法执行结果。
	 */
	public String getStrengthLevel() {
		return strengthLevel;
	}

	/**
	 * 设置相关属性值。
	 * @param strengthLevel strengthLevel参数。
	 */
	public void setStrengthLevel(String strengthLevel) {
		this.strengthLevel = strengthLevel;
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
