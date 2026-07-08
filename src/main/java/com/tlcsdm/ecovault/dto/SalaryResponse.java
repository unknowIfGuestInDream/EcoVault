package com.tlcsdm.ecovault.dto;

import java.math.BigDecimal;

/**
 * 工资数据响应。
 *
 * <p>
 * 除录入的分类明细外，附带服务端派生的应发工资、扣除项合计、税前/税后工资，以及单独记录的实发金额与年终奖标识。
 * </p>
 *
 * @param id 记录 ID
 * @param year 年份
 * @param month 月份 (0 表示年终奖)
 * @param annualBonus 是否为年终奖记录
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
 * @param grossPay 应发工资
 * @param totalDeduction 扣除项合计
 * @param preTaxSalary 税前工资
 * @param afterTaxSalary 税后工资
 * @param netPay 实发金额（本人银行卡实际到账）
 * @param remark 备注
 * @author unknowIfGuestInDream
 */
public record SalaryResponse(Long id, int year, int month, boolean annualBonus, BigDecimal baseSalary,
		BigDecimal performanceSalary, BigDecimal housingAllowance, BigDecimal mealAllowance,
		BigDecimal transportAllowance, BigDecimal overtimePay, BigDecimal overtimeAllowance, BigDecimal bonus,
		BigDecimal medicalBase, BigDecimal pensionUnemploymentBase, BigDecimal housingFundBase,
		BigDecimal medicalDeduction, BigDecimal pensionDeduction, BigDecimal unemploymentDeduction,
		BigDecimal housingFundDeduction, BigDecimal incomeTax, BigDecimal seriousIllnessMedical,
		BigDecimal heatingAllowance, BigDecimal grossPay, BigDecimal totalDeduction, BigDecimal preTaxSalary,
		BigDecimal afterTaxSalary, BigDecimal netPay, String remark) {
}
