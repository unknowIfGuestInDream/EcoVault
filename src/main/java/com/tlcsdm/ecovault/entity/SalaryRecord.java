package com.tlcsdm.ecovault.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 工资数据实体 (财务管理模块)。
 *
 * <p>
 * 按工资条分类记录用户每月的工资构成：发放项 (基本工资、绩效、各类补助、加班、奖金)、 社保/公积金缴费基数、扣除项
 * (医疗、养老、失业、公积金)、所得税，以及大病医疗、采暖补贴与实发金额等。 由发放项派生「应发工资」，由扣除项派生「扣除项合计」，进一步派生税前工资与税后工资；
 * 「实发金额」单独记录本人银行卡实际到账金额，用于统计分析与趋势图表。
 * </p>
 *
 * <p>
 * 月份 {@code month} 取值 1-12 表示当月工资；取值 0 表示该年度的「年终奖」记录，需额外录入。 通过 (user_id, year, month)
 * 唯一约束保证每人每年每月 (含年终奖) 仅一条记录。
 * </p>
 *
 * @author unknowIfGuestInDream
 */
@Entity
@Table(name = "salary_records",
		uniqueConstraints = @UniqueConstraint(name = "uk_salary_user_ym", columnNames = { "user_id", "year", "month" }),
		indexes = { @Index(name = "idx_salary_user", columnList = "user_id"),
				@Index(name = "idx_salary_ym", columnList = "year,month") })
/**
 * 工资记录实体，表示用户某年某月或年终奖的工资明细。 保存工资组成项、扣除项及统计所需的派生金额。
 *
 * @author unknowIfGuestInDream
 * @since 1.0.0
 */
public class SalaryRecord {

	/** 表示年终奖记录的特殊月份值 */
	public static final int ANNUAL_BONUS_MONTH = 0;

	/** 主键 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 所属用户 ID */
	@Column(name = "user_id", nullable = false)
	private Long userId;

	/** 年份 */
	@Column(nullable = false)
	private int year;

	/** 月份 (1-12)；0 表示年终奖 */
	@Column(nullable = false)
	private int month;

	// ===== 发放项 =====

	/** 基本工资 */
	@Column(name = "base_salary", nullable = false, precision = 12, scale = 2)
	private BigDecimal baseSalary = BigDecimal.ZERO;

	/** 绩效工资 */
	@Column(name = "performance_salary", nullable = false, precision = 12, scale = 2)
	private BigDecimal performanceSalary = BigDecimal.ZERO;

	/** 租房补助 */
	@Column(name = "housing_allowance", nullable = false, precision = 12, scale = 2)
	private BigDecimal housingAllowance = BigDecimal.ZERO;

	/** 伙食补助 */
	@Column(name = "meal_allowance", nullable = false, precision = 12, scale = 2)
	private BigDecimal mealAllowance = BigDecimal.ZERO;

	/** 交通补贴 */
	@Column(name = "transport_allowance", nullable = false, precision = 12, scale = 2)
	private BigDecimal transportAllowance = BigDecimal.ZERO;

	/** 加班费 */
	@Column(name = "overtime_pay", nullable = false, precision = 12, scale = 2)
	private BigDecimal overtimePay = BigDecimal.ZERO;

	/** 加班补助 */
	@Column(name = "overtime_allowance", nullable = false, precision = 12, scale = 2)
	private BigDecimal overtimeAllowance = BigDecimal.ZERO;

	/** 奖金 */
	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal bonus = BigDecimal.ZERO;

	// ===== 缴费基数 =====

	/** 医疗保险缴费基数 */
	@Column(name = "medical_base", nullable = false, precision = 12, scale = 2)
	private BigDecimal medicalBase = BigDecimal.ZERO;

	/** 养老失业缴费基数 */
	@Column(name = "pension_unemployment_base", nullable = false, precision = 12, scale = 2)
	private BigDecimal pensionUnemploymentBase = BigDecimal.ZERO;

	/** 公积金缴费基数 */
	@Column(name = "housing_fund_base", nullable = false, precision = 12, scale = 2)
	private BigDecimal housingFundBase = BigDecimal.ZERO;

	// ===== 扣除项 =====

	/** 医疗扣除 */
	@Column(name = "medical_deduction", nullable = false, precision = 12, scale = 2)
	private BigDecimal medicalDeduction = BigDecimal.ZERO;

	/** 养老扣除 */
	@Column(name = "pension_deduction", nullable = false, precision = 12, scale = 2)
	private BigDecimal pensionDeduction = BigDecimal.ZERO;

	/** 失业扣除 */
	@Column(name = "unemployment_deduction", nullable = false, precision = 12, scale = 2)
	private BigDecimal unemploymentDeduction = BigDecimal.ZERO;

	/** 公积金扣除 */
	@Column(name = "housing_fund_deduction", nullable = false, precision = 12, scale = 2)
	private BigDecimal housingFundDeduction = BigDecimal.ZERO;

	/** 所得税 */
	@Column(name = "income_tax", nullable = false, precision = 12, scale = 2)
	private BigDecimal incomeTax = BigDecimal.ZERO;

	// ===== 到账相关项 =====

	/** 大病医疗 */
	@Column(name = "serious_illness_medical", nullable = false, precision = 12, scale = 2)
	private BigDecimal seriousIllnessMedical = BigDecimal.ZERO;

	/** 采暖补贴 */
	@Column(name = "heating_allowance", nullable = false, precision = 12, scale = 2)
	private BigDecimal heatingAllowance = BigDecimal.ZERO;

	/** 实发金额（本人银行卡实际到账） */
	@Column(name = "net_pay", nullable = false, precision = 12, scale = 2)
	private BigDecimal netPay = BigDecimal.ZERO;

	/**
	 * 应发工资（可由基础发放项加总派生，或从外部数据（如 CSV 导入）直接存储）。
	 *
	 * <p>
	 * 若为 null，则由 {@link #getGrossPay()} 实时计算。
	 * </p>
	 */
	@Column(name = "gross_pay", precision = 12, scale = 2)
	private BigDecimal grossPay;

	/**
	 * 扣除项合计（可由各项扣除加总派生，或从外部数据直接存储）。
	 *
	 * <p>
	 * 若为 null，则由 {@link #getTotalDeduction()} 实时计算。
	 * </p>
	 */
	@Column(name = "total_deduction", precision = 12, scale = 2)
	private BigDecimal totalDeduction;

	/**
	 * 税前工资（可派生，或从外部数据直接存储）。
	 *
	 * <p>
	 * 若为 null，则由 {@link #getPreTaxSalary()} 实时计算。
	 * </p>
	 */
	@Column(name = "pre_tax_salary", precision = 12, scale = 2)
	private BigDecimal preTaxSalary;

	/**
	 * 税后工资（可派生，或从外部数据直接存储）。
	 *
	 * <p>
	 * 若为 null，则由 {@link #getAfterTaxSalary()} 实时计算。
	 * </p>
	 */
	@Column(name = "after_tax_salary", precision = 12, scale = 2)
	private BigDecimal afterTaxSalary;

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
	 * 是否为年终奖记录。
	 * @return true 表示年终奖记录
	 */
	public boolean isAnnualBonus() {
		return month == ANNUAL_BONUS_MONTH;
	}

	/**
	 * 计算应发工资 = 各发放项之和 (基本工资、绩效、租房/伙食补助、交通补贴、加班费、加班补助、奖金)。
	 *
	 * <p>
	 * 若字段已存储（如 CSV 导入），直接返回存储值；否则实时计算。
	 * </p>
	 * @return 应发工资
	 */
	public BigDecimal getGrossPay() {
		if (grossPay != null) {
			return grossPay;
		}
		return nullToZero(baseSalary).add(nullToZero(performanceSalary))
			.add(nullToZero(housingAllowance))
			.add(nullToZero(mealAllowance))
			.add(nullToZero(transportAllowance))
			.add(nullToZero(overtimePay))
			.add(nullToZero(overtimeAllowance))
			.add(nullToZero(bonus));
	}

	/**
	 * 计算扣除项合计 = 医疗 + 养老 + 失业 + 公积金。
	 *
	 * <p>
	 * 若字段已存储（如 CSV 导入），直接返回存储值；否则实时计算。
	 * </p>
	 * @return 扣除项合计
	 */
	public BigDecimal getTotalDeduction() {
		if (totalDeduction != null) {
			return totalDeduction;
		}
		return nullToZero(medicalDeduction).add(nullToZero(pensionDeduction))
			.add(nullToZero(unemploymentDeduction))
			.add(nullToZero(housingFundDeduction));
	}

	/**
	 * 计算税前工资 = 应发工资 - 扣除项合计。
	 *
	 * <p>
	 * 若字段已存储（如 CSV 导入），直接返回存储值；否则实时计算。
	 * </p>
	 * @return 税前工资
	 */
	public BigDecimal getPreTaxSalary() {
		if (preTaxSalary != null) {
			return preTaxSalary;
		}
		return getGrossPay().subtract(getTotalDeduction());
	}

	/**
	 * 计算税后工资 = 税前工资 - 所得税。
	 *
	 * <p>
	 * 若字段已存储（如 CSV 导入），直接返回存储值；否则实时计算。
	 * </p>
	 * @return 税后工资
	 */
	public BigDecimal getAfterTaxSalary() {
		if (afterTaxSalary != null) {
			return afterTaxSalary;
		}
		return getPreTaxSalary().subtract(nullToZero(incomeTax));
	}

	/**
	 * 获取实发金额（本人银行卡实际到账）。
	 * @return 实发金额
	 */
	public BigDecimal getNetPay() {
		return nullToZero(netPay);
	}

	private BigDecimal nullToZero(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
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
	public int getYear() {
		return year;
	}

	/**
	 * 设置相关属性值。
	 * @param year year参数。
	 */
	public void setYear(int year) {
		this.year = year;
	}

	/**
	 * 获取相关属性值。
	 * @return 方法执行结果。
	 */
	public int getMonth() {
		return month;
	}

	/**
	 * 设置相关属性值。
	 * @param month month参数。
	 */
	public void setMonth(int month) {
		this.month = month;
	}

	/**
	 * 获取相关属性值。
	 * @return 方法执行结果。
	 */
	public BigDecimal getBaseSalary() {
		return baseSalary;
	}

	/**
	 * 设置相关属性值。
	 * @param baseSalary baseSalary参数。
	 */
	public void setBaseSalary(BigDecimal baseSalary) {
		this.baseSalary = baseSalary;
	}

	/**
	 * 获取相关属性值。
	 * @return 方法执行结果。
	 */
	public BigDecimal getPerformanceSalary() {
		return performanceSalary;
	}

	/**
	 * 设置相关属性值。
	 * @param performanceSalary performanceSalary参数。
	 */
	public void setPerformanceSalary(BigDecimal performanceSalary) {
		this.performanceSalary = performanceSalary;
	}

	/**
	 * 获取相关属性值。
	 * @return 方法执行结果。
	 */
	public BigDecimal getHousingAllowance() {
		return housingAllowance;
	}

	/**
	 * 设置相关属性值。
	 * @param housingAllowance housingAllowance参数。
	 */
	public void setHousingAllowance(BigDecimal housingAllowance) {
		this.housingAllowance = housingAllowance;
	}

	/**
	 * 获取相关属性值。
	 * @return 方法执行结果。
	 */
	public BigDecimal getMealAllowance() {
		return mealAllowance;
	}

	/**
	 * 设置相关属性值。
	 * @param mealAllowance mealAllowance参数。
	 */
	public void setMealAllowance(BigDecimal mealAllowance) {
		this.mealAllowance = mealAllowance;
	}

	/**
	 * 获取相关属性值。
	 * @return 方法执行结果。
	 */
	public BigDecimal getTransportAllowance() {
		return transportAllowance;
	}

	/**
	 * 设置相关属性值。
	 * @param transportAllowance transportAllowance参数。
	 */
	public void setTransportAllowance(BigDecimal transportAllowance) {
		this.transportAllowance = transportAllowance;
	}

	/**
	 * 获取相关属性值。
	 * @return 方法执行结果。
	 */
	public BigDecimal getOvertimePay() {
		return overtimePay;
	}

	/**
	 * 设置相关属性值。
	 * @param overtimePay overtimePay参数。
	 */
	public void setOvertimePay(BigDecimal overtimePay) {
		this.overtimePay = overtimePay;
	}

	/**
	 * 获取相关属性值。
	 * @return 方法执行结果。
	 */
	public BigDecimal getOvertimeAllowance() {
		return overtimeAllowance;
	}

	/**
	 * 设置相关属性值。
	 * @param overtimeAllowance overtimeAllowance参数。
	 */
	public void setOvertimeAllowance(BigDecimal overtimeAllowance) {
		this.overtimeAllowance = overtimeAllowance;
	}

	/**
	 * 获取相关属性值。
	 * @return 方法执行结果。
	 */
	public BigDecimal getBonus() {
		return bonus;
	}

	/**
	 * 设置相关属性值。
	 * @param bonus bonus参数。
	 */
	public void setBonus(BigDecimal bonus) {
		this.bonus = bonus;
	}

	/**
	 * 获取相关属性值。
	 * @return 方法执行结果。
	 */
	public BigDecimal getMedicalBase() {
		return medicalBase;
	}

	/**
	 * 设置相关属性值。
	 * @param medicalBase medicalBase参数。
	 */
	public void setMedicalBase(BigDecimal medicalBase) {
		this.medicalBase = medicalBase;
	}

	/**
	 * 获取相关属性值。
	 * @return 方法执行结果。
	 */
	public BigDecimal getPensionUnemploymentBase() {
		return pensionUnemploymentBase;
	}

	/**
	 * 设置相关属性值。
	 * @param pensionUnemploymentBase pensionUnemploymentBase参数。
	 */
	public void setPensionUnemploymentBase(BigDecimal pensionUnemploymentBase) {
		this.pensionUnemploymentBase = pensionUnemploymentBase;
	}

	/**
	 * 获取相关属性值。
	 * @return 方法执行结果。
	 */
	public BigDecimal getHousingFundBase() {
		return housingFundBase;
	}

	/**
	 * 设置相关属性值。
	 * @param housingFundBase housingFundBase参数。
	 */
	public void setHousingFundBase(BigDecimal housingFundBase) {
		this.housingFundBase = housingFundBase;
	}

	/**
	 * 获取相关属性值。
	 * @return 方法执行结果。
	 */
	public BigDecimal getMedicalDeduction() {
		return medicalDeduction;
	}

	/**
	 * 设置相关属性值。
	 * @param medicalDeduction medicalDeduction参数。
	 */
	public void setMedicalDeduction(BigDecimal medicalDeduction) {
		this.medicalDeduction = medicalDeduction;
	}

	/**
	 * 获取相关属性值。
	 * @return 方法执行结果。
	 */
	public BigDecimal getPensionDeduction() {
		return pensionDeduction;
	}

	/**
	 * 设置相关属性值。
	 * @param pensionDeduction pensionDeduction参数。
	 */
	public void setPensionDeduction(BigDecimal pensionDeduction) {
		this.pensionDeduction = pensionDeduction;
	}

	/**
	 * 获取相关属性值。
	 * @return 方法执行结果。
	 */
	public BigDecimal getUnemploymentDeduction() {
		return unemploymentDeduction;
	}

	/**
	 * 设置相关属性值。
	 * @param unemploymentDeduction unemploymentDeduction参数。
	 */
	public void setUnemploymentDeduction(BigDecimal unemploymentDeduction) {
		this.unemploymentDeduction = unemploymentDeduction;
	}

	/**
	 * 获取相关属性值。
	 * @return 方法执行结果。
	 */
	public BigDecimal getHousingFundDeduction() {
		return housingFundDeduction;
	}

	/**
	 * 设置相关属性值。
	 * @param housingFundDeduction housingFundDeduction参数。
	 */
	public void setHousingFundDeduction(BigDecimal housingFundDeduction) {
		this.housingFundDeduction = housingFundDeduction;
	}

	/**
	 * 获取相关属性值。
	 * @return 方法执行结果。
	 */
	public BigDecimal getIncomeTax() {
		return incomeTax;
	}

	/**
	 * 设置相关属性值。
	 * @param incomeTax incomeTax参数。
	 */
	public void setIncomeTax(BigDecimal incomeTax) {
		this.incomeTax = incomeTax;
	}

	/**
	 * 获取相关属性值。
	 * @return 方法执行结果。
	 */
	public BigDecimal getSeriousIllnessMedical() {
		return seriousIllnessMedical;
	}

	/**
	 * 设置相关属性值。
	 * @param seriousIllnessMedical seriousIllnessMedical参数。
	 */
	public void setSeriousIllnessMedical(BigDecimal seriousIllnessMedical) {
		this.seriousIllnessMedical = seriousIllnessMedical;
	}

	/**
	 * 获取相关属性值。
	 * @return 方法执行结果。
	 */
	public BigDecimal getHeatingAllowance() {
		return heatingAllowance;
	}

	/**
	 * 设置相关属性值。
	 * @param heatingAllowance heatingAllowance参数。
	 */
	public void setHeatingAllowance(BigDecimal heatingAllowance) {
		this.heatingAllowance = heatingAllowance;
	}

	/**
	 * 设置相关属性值。
	 * @param netPay netPay参数。
	 */
	public void setNetPay(BigDecimal netPay) {
		this.netPay = netPay;
	}

	/**
	 * 获取相关属性值。
	 * @return 方法执行结果。
	 */
	public BigDecimal getStoredGrossPay() {
		return grossPay;
	}

	/**
	 * 设置相关属性值。
	 * @param grossPay grossPay参数。
	 */
	public void setStoredGrossPay(BigDecimal grossPay) {
		this.grossPay = grossPay;
	}

	/**
	 * 获取相关属性值。
	 * @return 方法执行结果。
	 */
	public BigDecimal getStoredTotalDeduction() {
		return totalDeduction;
	}

	/**
	 * 设置相关属性值。
	 * @param totalDeduction totalDeduction参数。
	 */
	public void setStoredTotalDeduction(BigDecimal totalDeduction) {
		this.totalDeduction = totalDeduction;
	}

	/**
	 * 获取相关属性值。
	 * @return 方法执行结果。
	 */
	public BigDecimal getStoredPreTaxSalary() {
		return preTaxSalary;
	}

	/**
	 * 设置相关属性值。
	 * @param preTaxSalary preTaxSalary参数。
	 */
	public void setStoredPreTaxSalary(BigDecimal preTaxSalary) {
		this.preTaxSalary = preTaxSalary;
	}

	/**
	 * 获取相关属性值。
	 * @return 方法执行结果。
	 */
	public BigDecimal getStoredAfterTaxSalary() {
		return afterTaxSalary;
	}

	/**
	 * 设置相关属性值。
	 * @param afterTaxSalary afterTaxSalary参数。
	 */
	public void setStoredAfterTaxSalary(BigDecimal afterTaxSalary) {
		this.afterTaxSalary = afterTaxSalary;
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
