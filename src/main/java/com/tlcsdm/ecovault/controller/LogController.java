package com.tlcsdm.ecovault.controller;

import com.tlcsdm.ecovault.annotation.OperationLogRecord;
import com.tlcsdm.ecovault.common.ApiResponse;
import com.tlcsdm.ecovault.config.DateTimeConfig;
import com.tlcsdm.ecovault.dto.UpdateLogRequest;
import com.tlcsdm.ecovault.entity.OperationLog;
import com.tlcsdm.ecovault.service.OperationLogService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 日志管理接口。
 *
 * <p>
 * 操作日志仅供管理员查看与管理 (由安全配置限制 {@code /api/logs/**} 为 ADMIN 权限)。
 * 支持模块、关键字、时间区间查询，日志详情查看，以及修改与删除。
 * </p>
 *
 * @author unknowIfGuestInDream
 */
@RestController
@RequestMapping("/api/logs")
public class LogController {

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(DateTimeConfig.DATE_TIME_PATTERN);

	private final OperationLogService operationLogService;

	public LogController(OperationLogService operationLogService) {
		this.operationLogService = operationLogService;
	}

	/**
	 * 分页查询日志。
	 * @param module 模块 (可空)
	 * @param keyword 关键字 (可空)
	 * @param start 起始时间 (可空，格式 yyyy-MM-dd HH:mm:ss)
	 * @param end 结束时间 (可空，格式 yyyy-MM-dd HH:mm:ss)
	 * @param page 页码 (从 0 开始)
	 * @param size 每页大小
	 * @return 分页日志
	 */
	@GetMapping
	public ApiResponse<Page<OperationLog>> list(@RequestParam(required = false) String module,
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) @DateTimeFormat(
					pattern = DateTimeConfig.DATE_TIME_PATTERN) LocalDateTime start,
			@RequestParam(required = false) @DateTimeFormat(
					pattern = DateTimeConfig.DATE_TIME_PATTERN) LocalDateTime end,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100));
		Page<OperationLog> result = operationLogService.query(null, module, keyword, start, end, pageable);
		return ApiResponse.success(result);
	}

	/**
	 * 查看日志详情 (含请求参数)。
	 * @param id 日志 ID
	 * @return 日志详情
	 */
	@GetMapping("/{id}")
	public ApiResponse<OperationLog> detail(@PathVariable Long id) {
		return ApiResponse.success(operationLogService.getById(id));
	}

	/**
	 * 修改日志的模块与操作描述。
	 * @param id 日志 ID
	 * @param request 更新请求
	 * @return 更新后的日志
	 */
	@PutMapping("/{id}")
	@OperationLogRecord(module = "日志管理", operation = "修改日志")
	public ApiResponse<OperationLog> update(@PathVariable Long id, @Valid @RequestBody UpdateLogRequest request) {
		return ApiResponse.success(operationLogService.update(id, request.module(), request.operation()));
	}

	/**
	 * 删除日志。
	 * @param id 日志 ID
	 * @return 操作结果
	 */
	@DeleteMapping("/{id}")
	@OperationLogRecord(module = "日志管理", operation = "删除日志")
	public ApiResponse<Void> delete(@PathVariable Long id) {
		operationLogService.delete(id);
		return ApiResponse.success();
	}

	/**
	 * 导出日志为 CSV。
	 * @param module 模块 (可空)
	 * @param keyword 关键字 (可空)
	 * @param start 起始时间 (可空)
	 * @param end 结束时间 (可空)
	 * @return CSV 文件
	 */
	@GetMapping("/export")
	public ResponseEntity<byte[]> export(@RequestParam(required = false) String module,
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) @DateTimeFormat(
					pattern = DateTimeConfig.DATE_TIME_PATTERN) LocalDateTime start,
			@RequestParam(required = false) @DateTimeFormat(
					pattern = DateTimeConfig.DATE_TIME_PATTERN) LocalDateTime end) {
		Pageable pageable = PageRequest.of(0, 10000);
		List<OperationLog> logs = operationLogService.query(null, module, keyword, start, end, pageable).getContent();

		StringBuilder sb = new StringBuilder();
		sb.append('\uFEFF');
		sb.append("时间,用户,模块,操作,方法,IP,状态,耗时(ms)\n");
		for (OperationLog log : logs) {
			sb.append(log.getCreatedAt() == null ? "" : FORMATTER.format(log.getCreatedAt()))
				.append(',')
				.append(nullToEmpty(log.getUsername()))
				.append(',')
				.append(nullToEmpty(log.getModule()))
				.append(',')
				.append(nullToEmpty(log.getOperation()))
				.append(',')
				.append(nullToEmpty(log.getMethod()))
				.append(',')
				.append(nullToEmpty(log.getIp()))
				.append(',')
				.append(nullToEmpty(log.getStatus()))
				.append(',')
				.append(log.getDurationMs())
				.append('\n');
		}
		byte[] body = sb.toString().getBytes(StandardCharsets.UTF_8);
		return ResponseEntity.ok()
			.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"operation-logs.csv\"")
			.contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
			.body(body);
	}

	private String nullToEmpty(String value) {
		return value == null ? "" : value;
	}

}
