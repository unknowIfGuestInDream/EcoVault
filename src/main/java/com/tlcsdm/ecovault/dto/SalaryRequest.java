package com.tlcsdm.ecovault.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 工资数据录入/编辑请求。
 *
 * <p>
 * 按工资条分类录入发放项、缴费基数、扣除项、所得税与税后附加项。 月份取值 1-12 表示当月工资；取值 0 表示该年度的年终奖记录。
 * 应发工资、扣除项合计、税前/税后工资与实发金额由服务端派生计算。
 * </p>
 *
 * @param year 年份
 * @param month 月份 (1-12)，0 表示年终奖
 * @param baseSalary 基本工资
 * @param performanceSalary 绩效工资
 * @param housingAllowance 租房补助
 * @param mealAllowance 伙食补助
 * @param transportAllowance 交通补贴
 * @param overtimePay 加班费
 * @param overtimeAllowance 加班补助
 * @param bonus 奖金
 * @param medicalBase 医疗保险缴费基数
 * @param pensionUnemploymentBase 养老失业缴费基数
 * @param housingFundBase 公积金缴费基数
 * @param medicalDeduction 医疗扣除
 * @param pensionDeduction 养老扣除
 * @param unemploymentDeduction 失业扣除
 * @param housingFundDeduction 公积金扣除
 * @param incomeTax 所得税
 * @param seriousIllnessMedical 大病医疗
 * @param heatingAllowance 采暖补贴
 * @param remark 备注
 * @author unknowIfGuestInDream
 */
public record SalaryRequest(
		@NotNull(message = "年份不能为空") @Min(value = 1970, message = "年份不合法") @Max(value = 9999,
				message = "年份不合法") Integer year,

		@NotNull(message = "月份不能为空") @Min(value = 0, message = "月份需在 0-12 之间 (0 表示年终奖)") @Max(value = 12,
				message = "月份需在 0-12 之间 (0 表示年终奖)") Integer month,

		BigDecimal baseSalary, BigDecimal performanceSalary, BigDecimal housingAllowance, BigDecimal mealAllowance,
		BigDecimal transportAllowance, BigDecimal overtimePay, BigDecimal overtimeAllowance, BigDecimal bonus,
		BigDecimal medicalBase, BigDecimal pensionUnemploymentBase, BigDecimal housingFundBase,
		BigDecimal medicalDeduction, BigDecimal pensionDeduction, BigDecimal unemploymentDeduction,
		BigDecimal housingFundDeduction, BigDecimal incomeTax, BigDecimal seriousIllnessMedical,
		BigDecimal heatingAllowance, String remark) {
}
