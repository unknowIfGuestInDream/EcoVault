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

	/**
	 * 是否为年终奖记录。
	 * @return true 表示年终奖记录
	 */
	public boolean isAnnualBonus() {
		return month == ANNUAL_BONUS_MONTH;
	}

	/**
	 * 计算应发工资 = 各发放项之和 (基本工资、绩效、租房/伙食补助、交通补贴、加班费、加班补助、奖金)。
	 * @return 应发工资
	 */
	public BigDecimal getGrossPay() {
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
	 * @return 扣除项合计
	 */
	public BigDecimal getTotalDeduction() {
		return nullToZero(medicalDeduction).add(nullToZero(pensionDeduction))
			.add(nullToZero(unemploymentDeduction))
			.add(nullToZero(housingFundDeduction));
	}

	/**
	 * 计算税前工资 = 应发工资 - 扣除项合计。
	 * @return 税前工资
	 */
	public BigDecimal getPreTaxSalary() {
		return getGrossPay().subtract(getTotalDeduction());
	}

	/**
	 * 计算税后工资 = 税前工资 - 所得税。
	 * @return 税后工资
	 */
	public BigDecimal getAfterTaxSalary() {
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

	public int getYear() {
		return year;
	}

	public void setYear(int year) {
		this.year = year;
	}

	public int getMonth() {
		return month;
	}

	public void setMonth(int month) {
		this.month = month;
	}

	public BigDecimal getBaseSalary() {
		return baseSalary;
	}

	public void setBaseSalary(BigDecimal baseSalary) {
		this.baseSalary = baseSalary;
	}

	public BigDecimal getPerformanceSalary() {
		return performanceSalary;
	}

	public void setPerformanceSalary(BigDecimal performanceSalary) {
		this.performanceSalary = performanceSalary;
	}

	public BigDecimal getHousingAllowance() {
		return housingAllowance;
	}

	public void setHousingAllowance(BigDecimal housingAllowance) {
		this.housingAllowance = housingAllowance;
	}

	public BigDecimal getMealAllowance() {
		return mealAllowance;
	}

	public void setMealAllowance(BigDecimal mealAllowance) {
		this.mealAllowance = mealAllowance;
	}

	public BigDecimal getTransportAllowance() {
		return transportAllowance;
	}

	public void setTransportAllowance(BigDecimal transportAllowance) {
		this.transportAllowance = transportAllowance;
	}

	public BigDecimal getOvertimePay() {
		return overtimePay;
	}

	public void setOvertimePay(BigDecimal overtimePay) {
		this.overtimePay = overtimePay;
	}

	public BigDecimal getOvertimeAllowance() {
		return overtimeAllowance;
	}

	public void setOvertimeAllowance(BigDecimal overtimeAllowance) {
		this.overtimeAllowance = overtimeAllowance;
	}

	public BigDecimal getBonus() {
		return bonus;
	}

	public void setBonus(BigDecimal bonus) {
		this.bonus = bonus;
	}

	public BigDecimal getMedicalBase() {
		return medicalBase;
	}

	public void setMedicalBase(BigDecimal medicalBase) {
		this.medicalBase = medicalBase;
	}

	public BigDecimal getPensionUnemploymentBase() {
		return pensionUnemploymentBase;
	}

	public void setPensionUnemploymentBase(BigDecimal pensionUnemploymentBase) {
		this.pensionUnemploymentBase = pensionUnemploymentBase;
	}

	public BigDecimal getHousingFundBase() {
		return housingFundBase;
	}

	public void setHousingFundBase(BigDecimal housingFundBase) {
		this.housingFundBase = housingFundBase;
	}

	public BigDecimal getMedicalDeduction() {
		return medicalDeduction;
	}

	public void setMedicalDeduction(BigDecimal medicalDeduction) {
		this.medicalDeduction = medicalDeduction;
	}

	public BigDecimal getPensionDeduction() {
		return pensionDeduction;
	}

	public void setPensionDeduction(BigDecimal pensionDeduction) {
		this.pensionDeduction = pensionDeduction;
	}

	public BigDecimal getUnemploymentDeduction() {
		return unemploymentDeduction;
	}

	public void setUnemploymentDeduction(BigDecimal unemploymentDeduction) {
		this.unemploymentDeduction = unemploymentDeduction;
	}

	public BigDecimal getHousingFundDeduction() {
		return housingFundDeduction;
	}

	public void setHousingFundDeduction(BigDecimal housingFundDeduction) {
		this.housingFundDeduction = housingFundDeduction;
	}

	public BigDecimal getIncomeTax() {
		return incomeTax;
	}

	public void setIncomeTax(BigDecimal incomeTax) {
		this.incomeTax = incomeTax;
	}

	public BigDecimal getSeriousIllnessMedical() {
		return seriousIllnessMedical;
	}

	public void setSeriousIllnessMedical(BigDecimal seriousIllnessMedical) {
		this.seriousIllnessMedical = seriousIllnessMedical;
	}

	public BigDecimal getHeatingAllowance() {
		return heatingAllowance;
	}

	public void setHeatingAllowance(BigDecimal heatingAllowance) {
		this.heatingAllowance = heatingAllowance;
	}

	public void setNetPay(BigDecimal netPay) {
		this.netPay = netPay;
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
