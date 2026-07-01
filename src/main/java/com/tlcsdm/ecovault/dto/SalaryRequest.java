package com.tlcsdm.ecovault.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 工资数据录入/编辑请求。
 *
 * @param year 年份
 * @param month 月份 (1-12)
 * @param baseSalary 基本工资
 * @param bonus 奖金
 * @param allowance 补贴
 * @param deduction 扣款
 * @param remark 备注
 * @author unknowIfGuestInDream
 */
public record SalaryRequest(
		@NotNull(message = "年份不能为空") @Min(value = 1970, message = "年份不合法") @Max(value = 9999,
				message = "年份不合法") Integer year,

		@NotNull(message = "月份不能为空") @Min(value = 1, message = "月份需在 1-12 之间") @Max(value = 12,
				message = "月份需在 1-12 之间") Integer month,

		BigDecimal baseSalary, BigDecimal bonus, BigDecimal allowance, BigDecimal deduction, String remark) {
}
