package com.tlcsdm.ecovault.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tlcsdm.ecovault.config.DateTimeConfig;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 收入支出记录录入/编辑请求。
 *
 * @param type 收支类型 (INCOME/EXPENSE)
 * @param amount 金额 (需大于 0)
 * @param entryDate 发生日期
 * @param tags 标签列表 (可多个，可空)
 * @param remark 备注
 * @author unknowIfGuestInDream
 */
public record LedgerRequest(@NotNull(message = "收支类型不能为空") String type,

		@NotNull(message = "金额不能为空") @DecimalMin(value = "0.01", message = "金额需大于 0") BigDecimal amount,

		@NotNull(message = "发生日期不能为空") @JsonFormat(pattern = DateTimeConfig.DATE_PATTERN,
				timezone = DateTimeConfig.TIME_ZONE) LocalDate entryDate,

		List<String> tags,

		String remark) {
}
