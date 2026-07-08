package com.tlcsdm.ecovault.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 工资统计分析结果。
 *
 * <p>
 * 净收入相关指标 (合计/均值/最高/最低/趋势) 基于工资记录中的实发金额 (本人银行卡实际到账) 计算， 年终奖 (month 0) 单独汇总为
 * {@code totalAnnualBonus}。
 * </p>
 *
 * @param totalNet 实发合计 (当月工资)
 * @param averageNet 月均实发
 * @param maxNet 最高实发
 * @param minNet 最低实发
 * @param totalBonus 奖金合计 (当月工资中的奖金发放项)
 * @param totalAnnualBonus 年终奖实发合计
 * @param monthlyTrend 月度趋势 (按时间排序，不含年终奖)
 * @param composition 发放项构成 (各发放项合计)
 * @param deductionComposition 扣除项构成 (各扣除项与所得税合计)
 * @author unknowIfGuestInDream
 */
public record SalaryStatistics(BigDecimal totalNet, BigDecimal averageNet, BigDecimal maxNet, BigDecimal minNet,
		BigDecimal totalBonus, BigDecimal totalAnnualBonus, List<MonthlyPoint> monthlyTrend, Composition composition,
		DeductionComposition deductionComposition) {

	/**
	 * 月度趋势数据点。
	 *
	 * @param label 标签 (yyyy-MM)
	 * @param net 实发金额
	 * @param gross 应发工资
	 */
	public record MonthlyPoint(String label, BigDecimal net, BigDecimal gross) {
	}

	/**
	 * 发放项构成 (用于收入构成图表)。
	 *
	 * @param baseSalary 基本工资合计
	 * @param performanceSalary 绩效工资合计
	 * @param housingAllowance 租房补助合计
	 * @param mealAllowance 伙食补助合计
	 * @param transportAllowance 交通补贴合计
	 * @param overtimePay 加班费合计
	 * @param overtimeAllowance 加班补助合计
	 * @param bonus 奖金合计
	 */
	public record Composition(BigDecimal baseSalary, BigDecimal performanceSalary, BigDecimal housingAllowance,
			BigDecimal mealAllowance, BigDecimal transportAllowance, BigDecimal overtimePay,
			BigDecimal overtimeAllowance, BigDecimal bonus) {
	}

	/**
	 * 扣除项构成 (用于扣除构成图表)。
	 *
	 * @param medical 医疗扣除合计
	 * @param pension 养老扣除合计
	 * @param unemployment 失业扣除合计
	 * @param housingFund 公积金扣除合计
	 * @param incomeTax 所得税合计
	 */
	public record DeductionComposition(BigDecimal medical, BigDecimal pension, BigDecimal unemployment,
			BigDecimal housingFund, BigDecimal incomeTax) {
	}
}
