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
/**
 * 收支记录实体，表示用户的一笔收入或支出流水。 保存金额、日期、标签和备注等财务信息。
 *
 * @author unknowIfGuestInDream
 * @since 1.0.0
 */
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
	public LedgerType getType() {
		return type;
	}

	/**
	 * 设置相关属性值。
	 * @param type 类型条件。
	 */
	public void setType(LedgerType type) {
		this.type = type;
	}

	/**
	 * 获取相关属性值。
	 * @return 方法执行结果。
	 */
	public BigDecimal getAmount() {
		return amount;
	}

	/**
	 * 设置相关属性值。
	 * @param amount amount参数。
	 */
	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	/**
	 * 获取相关属性值。
	 * @return 方法执行结果。
	 */
	public LocalDate getEntryDate() {
		return entryDate;
	}

	/**
	 * 设置相关属性值。
	 * @param entryDate entryDate参数。
	 */
	public void setEntryDate(LocalDate entryDate) {
		this.entryDate = entryDate;
	}

	/**
	 * 获取相关属性值。
	 * @return 方法执行结果。
	 */
	public Set<String> getTags() {
		return tags;
	}

	/**
	 * 设置相关属性值。
	 * @param tags tags参数。
	 */
	public void setTags(Set<String> tags) {
		this.tags = tags;
	}

	/**
	 * 获取相关属性值。
	 * @return 方法执行结果。
	 */
	public String getRemark() {
		return remark;
	}

	/**
	 * 设置相关属性值。
	 * @param remark remark参数。
	 */
	public void setRemark(String remark) {
		this.remark = remark;
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
