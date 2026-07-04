package com.tlcsdm.ecovault.dto;

import jakarta.validation.constraints.Size;

/**
 * 更新操作日志请求 (仅管理员)。
 *
 * @param module 模块
 * @param operation 操作描述
 * @author unknowIfGuestInDream
 */
public record UpdateLogRequest(@Size(max = 64, message = "模块名称过长") String module,

		@Size(max = 256, message = "操作描述过长") String operation) {
}
