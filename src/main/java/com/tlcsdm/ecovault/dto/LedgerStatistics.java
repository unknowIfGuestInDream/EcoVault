package com.tlcsdm.ecovault.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 收入支出统计结果。
 *
 * @param totalIncome 收入合计
 * @param totalExpense 支出合计
 * @param balance 结余 (收入合计 - 支出合计)
 * @param count 记录条数
 * @param incomeByTag 收入按标签汇总
 * @param expenseByTag 支出按标签汇总
 * @param monthlyTrend 按月收支趋势
 * @author unknowIfGuestInDream
 */
public record LedgerStatistics(BigDecimal totalIncome, BigDecimal totalExpense, BigDecimal balance, long count,
		List<TagAmount> incomeByTag, List<TagAmount> expenseByTag, List<MonthlyPoint> monthlyTrend) {

	/**
	 * 标签金额聚合。
	 *
	 * @param tag 标签
	 * @param amount 金额合计
	 */
	public record TagAmount(String tag, BigDecimal amount) {
	}

	/**
	 * 月度收支数据点。
	 *
	 * @param label 月份标签 (yyyy-MM)
	 * @param income 当月收入合计
	 * @param expense 当月支出合计
	 */
	public record MonthlyPoint(String label, BigDecimal income, BigDecimal expense) {
	}

}
