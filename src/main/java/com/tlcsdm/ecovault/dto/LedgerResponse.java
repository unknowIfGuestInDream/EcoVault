package com.tlcsdm.ecovault.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tlcsdm.ecovault.config.DateTimeConfig;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 收入支出记录响应。
 *
 * @param id 记录 ID
 * @param type 收支类型 (INCOME/EXPENSE)
 * @param amount 金额
 * @param entryDate 发生日期
 * @param tags 标签列表
 * @param remark 备注
 * @author unknowIfGuestInDream
 */
public record LedgerResponse(Long id, String type, BigDecimal amount,
		@JsonFormat(pattern = DateTimeConfig.DATE_PATTERN, timezone = DateTimeConfig.TIME_ZONE) LocalDate entryDate,
		List<String> tags, String remark) {
}
