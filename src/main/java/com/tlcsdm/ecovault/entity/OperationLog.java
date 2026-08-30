package com.tlcsdm.ecovault.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tlcsdm.ecovault.config.DateTimeConfig;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 操作日志实体。
 *
 * <p>
 * 由 AOP 切面自动记录用户在系统中的关键操作，用于审计与问题排查。 管理员可查看所有用户日志，普通用户仅可查看自己的日志。
 * </p>
 *
 * @author unknowIfGuestInDream
 */
@Entity
@Table(name = "operation_logs",
		indexes = { @Index(name = "idx_log_user", columnList = "user_id"),
				@Index(name = "idx_log_module", columnList = "module"),
				@Index(name = "idx_log_created", columnList = "created_at") })
/**
 * 操作日志实体，记录系统关键操作的审计信息。 保存操作模块、行为、用户、结果及请求上下文等内容。
 *
 * @author unknowIfGuestInDream
 * @since 1.0.0
 */
public class OperationLog {

	/** 主键 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 操作用户 ID (匿名操作可为空) */
	@Column(name = "user_id")
	private Long userId;

	/** 操作用户名 */
	@Column(length = 64)
	private String username;

	/** 所属模块 (如：用户管理、密码管理) */
	@Column(length = 64)
	private String module;

	/** 操作描述 */
	@Column(length = 256)
	private String operation;

	/** 执行的方法签名 */
	@Column(length = 256)
	private String method;

	/** 请求参数摘要 */
	@Lob
	private String params;

	/** 客户端 IP */
	@Column(length = 64)
	private String ip;

	/** 执行结果状态 (SUCCESS/FAILURE) */
	@Column(length = 16)
	private String status;

	/** 错误信息 (失败时) */
	@Lob
	@Column(name = "error_msg")
	private String errorMsg;

	/** 执行耗时 (毫秒) */
	@Column(name = "duration_ms")
	private long durationMs;

	/** 创建时间 */
	@JsonFormat(pattern = DateTimeConfig.DATE_TIME_PATTERN, timezone = DateTimeConfig.TIME_ZONE)
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	/**
	 * 在实体首次持久化前初始化时间等字段。
	 */
	@PrePersist
	public void prePersist() {
		this.createdAt = LocalDateTime.now();
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
	 * @return 方法执行结果。
	 */
	public String getModule() {
		return module;
	}

	/**
	 * 设置相关属性值。
	 * @param module module参数。
	 */
	public void setModule(String module) {
		this.module = module;
	}

	/**
	 * 获取相关属性值。
	 * @return 方法执行结果。
	 */
	public String getOperation() {
		return operation;
	}

	/**
	 * 设置相关属性值。
	 * @param operation operation参数。
	 */
	public void setOperation(String operation) {
		this.operation = operation;
	}

	/**
	 * 获取相关属性值。
	 * @return 方法执行结果。
	 */
	public String getMethod() {
		return method;
	}

	/**
	 * 设置相关属性值。
	 * @param method method参数。
	 */
	public void setMethod(String method) {
		this.method = method;
	}

	/**
	 * 获取相关属性值。
	 * @return 方法执行结果。
	 */
	public String getParams() {
		return params;
	}

	/**
	 * 设置相关属性值。
	 * @param params params参数。
	 */
	public void setParams(String params) {
		this.params = params;
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
	 * 获取相关属性值。
	 * @return 方法执行结果。
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * 设置相关属性值。
	 * @param status status参数。
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * 获取相关属性值。
	 * @return 方法执行结果。
	 */
	public String getErrorMsg() {
		return errorMsg;
	}

	/**
	 * 设置相关属性值。
	 * @param errorMsg errorMsg参数。
	 */
	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}

	/**
	 * 获取相关属性值。
	 * @return 方法执行结果。
	 */
	public long getDurationMs() {
		return durationMs;
	}

	/**
	 * 设置相关属性值。
	 * @param durationMs durationMs参数。
	 */
	public void setDurationMs(long durationMs) {
		this.durationMs = durationMs;
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

}
