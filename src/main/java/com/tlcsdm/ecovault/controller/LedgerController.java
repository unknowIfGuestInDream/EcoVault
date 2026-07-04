package com.tlcsdm.ecovault.controller;

import com.tlcsdm.ecovault.annotation.OperationLogRecord;
import com.tlcsdm.ecovault.common.ApiResponse;
import com.tlcsdm.ecovault.dto.LedgerRequest;
import com.tlcsdm.ecovault.dto.LedgerResponse;
import com.tlcsdm.ecovault.dto.LedgerStatistics;
import com.tlcsdm.ecovault.security.SecurityUtils;
import com.tlcsdm.ecovault.service.LedgerService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/**
 * 财务 - 收入支出管理接口。
 *
 * <p>
 * 支持记录用户每天的收入/支出，为其打上多个标签，并可按标签、时间区间与类型查询、统计与导出。
 * </p>
 *
 * @author unknowIfGuestInDream
 */
@RestController
@RequestMapping("/api/finance/ledger")
public class LedgerController {

	private final LedgerService ledgerService;

	public LedgerController(LedgerService ledgerService) {
		this.ledgerService = ledgerService;
	}

	/**
	 * 按条件查询收支记录。
	 * @param type 收支类型 (可空)
	 * @param start 起始日期 (可空)
	 * @param end 结束日期 (可空)
	 * @param tag 标签 (可空)
	 * @return 记录列表
	 */
	@GetMapping
	public ApiResponse<List<LedgerResponse>> list(@RequestParam(required = false) String type,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
			@RequestParam(required = false) String tag) {
		return ApiResponse.success(ledgerService.list(SecurityUtils.getCurrentUserId(), type, start, end, tag));
	}

	/**
	 * 收支统计分析。
	 * @param type 收支类型 (可空)
	 * @param start 起始日期 (可空)
	 * @param end 结束日期 (可空)
	 * @param tag 标签 (可空)
	 * @return 统计结果
	 */
	@GetMapping("/statistics")
	public ApiResponse<LedgerStatistics> statistics(@RequestParam(required = false) String type,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
			@RequestParam(required = false) String tag) {
		return ApiResponse.success(ledgerService.statistics(SecurityUtils.getCurrentUserId(), type, start, end, tag));
	}

	/**
	 * 新增收支记录。
	 * @param request 请求
	 * @return 保存结果
	 */
	@PostMapping
	@OperationLogRecord(module = "财务管理", operation = "新增收支记录")
	public ApiResponse<LedgerResponse> create(@Valid @RequestBody LedgerRequest request) {
		return ApiResponse.success(ledgerService.create(SecurityUtils.getCurrentUserId(), request));
	}

	/**
	 * 更新收支记录。
	 * @param id 记录 ID
	 * @param request 请求
	 * @return 更新结果
	 */
	@PutMapping("/{id}")
	@OperationLogRecord(module = "财务管理", operation = "更新收支记录")
	public ApiResponse<LedgerResponse> update(@PathVariable Long id, @Valid @RequestBody LedgerRequest request) {
		return ApiResponse.success(ledgerService.update(SecurityUtils.getCurrentUserId(), id, request));
	}

	/**
	 * 删除收支记录。
	 * @param id 记录 ID
	 * @return 删除结果
	 */
	@DeleteMapping("/{id}")
	@OperationLogRecord(module = "财务管理", operation = "删除收支记录")
	public ApiResponse<Void> delete(@PathVariable Long id) {
		ledgerService.delete(SecurityUtils.getCurrentUserId(), id);
		return ApiResponse.success();
	}

	/**
	 * 导出收支记录为 CSV 文件。
	 * @param type 收支类型 (可空)
	 * @param start 起始日期 (可空)
	 * @param end 结束日期 (可空)
	 * @param tag 标签 (可空)
	 * @return CSV 文件
	 */
	@GetMapping("/export")
	@OperationLogRecord(module = "财务管理", operation = "导出收支记录")
	public ResponseEntity<byte[]> export(@RequestParam(required = false) String type,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
			@RequestParam(required = false) String tag) {
		String csv = ledgerService.exportCsv(SecurityUtils.getCurrentUserId(), type, start, end, tag);
		byte[] body = csv.getBytes(StandardCharsets.UTF_8);
		return ResponseEntity.ok()
			.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"ledger.csv\"")
			.contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
			.body(body);
	}

}
