package com.tlcsdm.ecovault.controller;

import com.tlcsdm.ecovault.common.ApiResponse;
import com.tlcsdm.ecovault.entity.OperationLog;
import com.tlcsdm.ecovault.security.SecurityUtils;
import com.tlcsdm.ecovault.service.OperationLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 日志管理接口。
 *
 * <p>
 * 普通用户只能查看自己的操作日志；管理员可查看所有用户的日志。
 * </p>
 *
 * @author unknowIfGuestInDream
 */
@RestController
@RequestMapping("/api/logs")
public class LogController {

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final OperationLogService operationLogService;

	public LogController(OperationLogService operationLogService) {
		this.operationLogService = operationLogService;
	}

	/**
	 * 分页查询日志。
	 * @param module 模块 (可空)
	 * @param keyword 关键字 (可空)
	 * @param page 页码 (从 0 开始)
	 * @param size 每页大小
	 * @return 分页日志
	 */
	@GetMapping
	public ApiResponse<Page<OperationLog>> list(@RequestParam(required = false) String module,
			@RequestParam(required = false) String keyword, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100));
		Page<OperationLog> result = operationLogService.query(enforcedUserId(), module, keyword, pageable);
		return ApiResponse.success(result);
	}

	/**
	 * 导出日志为 CSV。
	 * @param module 模块 (可空)
	 * @param keyword 关键字 (可空)
	 * @return CSV 文件
	 */
	@GetMapping("/export")
	public ResponseEntity<byte[]> export(@RequestParam(required = false) String module,
			@RequestParam(required = false) String keyword) {
		Pageable pageable = PageRequest.of(0, 10000);
		List<OperationLog> logs = operationLogService.query(enforcedUserId(), module, keyword, pageable).getContent();

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

	/**
	 * 计算需强制限定的用户 ID：管理员为 null (查询全部)，普通用户为自身 ID。
	 * @return 限定用户 ID 或 null
	 */
	private Long enforcedUserId() {
		return SecurityUtils.isAdmin() ? null : SecurityUtils.getCurrentUserId();
	}

	private String nullToEmpty(String value) {
		return value == null ? "" : value;
	}

}
