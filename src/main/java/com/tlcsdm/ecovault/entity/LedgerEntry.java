package com.tlcsdm.ecovault.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 收入支出记录实体 (财务管理 - 收入支出管理)。
 *
 * <p>
 * 记录用户某一天的一笔收入或支出，可为其打上多个标签，便于按标签、时间等维度进行查询与统计分析。
 * </p>
 *
 * @author unknowIfGuestInDream
 */
@Entity
@Table(name = "ledger_entries",
		indexes = { @Index(name = "idx_ledger_user", columnList = "user_id"),
				@Index(name = "idx_ledger_date", columnList = "entry_date"),
				@Index(name = "idx_ledger_type", columnList = "type") })
public class LedgerEntry {

	/** 主键 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 所属用户 ID */
	@Column(name = "user_id", nullable = false)
	private Long userId;

	/** 收支类型 (收入/支出) */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private LedgerType type;

	/** 金额 (正数) */
	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal amount = BigDecimal.ZERO;

	/** 发生日期 */
	@Column(name = "entry_date", nullable = false)
	private LocalDate entryDate;

	/** 标签集合 (可多个) */
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "ledger_entry_tags", joinColumns = @JoinColumn(name = "entry_id"),
			indexes = @Index(name = "idx_ledger_tag", columnList = "tag"))
	@Column(name = "tag", length = 32)
	private Set<String> tags = new LinkedHashSet<>();

	/** 备注 */
	@Column(length = 256)
	private String remark;

	/** 创建时间 */
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	/** 更新时间 */
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	public void prePersist() {
		LocalDateTime now = LocalDateTime.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

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

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public LedgerType getType() {
		return type;
	}

	public void setType(LedgerType type) {
		this.type = type;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public LocalDate getEntryDate() {
		return entryDate;
	}

	public void setEntryDate(LocalDate entryDate) {
		this.entryDate = entryDate;
	}

	public Set<String> getTags() {
		return tags;
	}

	public void setTags(Set<String> tags) {
		this.tags = tags;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
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
