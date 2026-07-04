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

	@PrePersist
	public void prePersist() {
		this.createdAt = LocalDateTime.now();
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

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getModule() {
		return module;
	}

	public void setModule(String module) {
		this.module = module;
	}

	public String getOperation() {
		return operation;
	}

	public void setOperation(String operation) {
		this.operation = operation;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getParams() {
		return params;
	}

	public void setParams(String params) {
		this.params = params;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getErrorMsg() {
		return errorMsg;
	}

	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}

	public long getDurationMs() {
		return durationMs;
	}

	public void setDurationMs(long durationMs) {
		this.durationMs = durationMs;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

}
