package com.tlcsdm.ecovault.dto;

import java.math.BigDecimal;

/**
 * 工资数据响应。
 *
 * @param id         记录 ID
 * @param year       年份
 * @param month      月份
 * @param baseSalary 基本工资
 * @param bonus      奖金
 * @param allowance  补贴
 * @param deduction  扣款
 * @param gross      税前总额
 * @param net        实发金额
 * @param remark     备注
 * @author 梦里不知身是客
 */
public record SalaryResponse(
        Long id,
        int year,
        int month,
        BigDecimal baseSalary,
        BigDecimal bonus,
        BigDecimal allowance,
        BigDecimal deduction,
        BigDecimal gross,
        BigDecimal net,
        String remark
) {
}
