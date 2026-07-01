package com.tlcsdm.ecovault.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 工资统计分析结果。
 *
 * @param totalNet      实发合计
 * @param averageNet    月均实发
 * @param maxNet        最高实发
 * @param minNet        最低实发
 * @param totalBonus    奖金合计
 * @param monthlyTrend  月度趋势 (按时间排序)
 * @param composition   收入构成 (基本工资/奖金/补贴合计)
 * @author unknowIfGuestInDream
 */
public record SalaryStatistics(
        BigDecimal totalNet,
        BigDecimal averageNet,
        BigDecimal maxNet,
        BigDecimal minNet,
        BigDecimal totalBonus,
        List<MonthlyPoint> monthlyTrend,
        Composition composition
) {

    /**
     * 月度趋势数据点。
     *
     * @param label 标签 (yyyy-MM)
     * @param net   实发金额
     * @param gross 税前总额
     */
    public record MonthlyPoint(String label, BigDecimal net, BigDecimal gross) {
    }

    /**
     * 收入构成。
     *
     * @param baseSalary 基本工资合计
     * @param bonus      奖金合计
     * @param allowance  补贴合计
     */
    public record Composition(BigDecimal baseSalary, BigDecimal bonus, BigDecimal allowance) {
    }
}
