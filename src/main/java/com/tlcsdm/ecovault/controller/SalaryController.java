package com.tlcsdm.ecovault.controller;

import com.tlcsdm.ecovault.annotation.OperationLogRecord;
import com.tlcsdm.ecovault.common.ApiResponse;
import com.tlcsdm.ecovault.dto.SalaryRequest;
import com.tlcsdm.ecovault.dto.SalaryResponse;
import com.tlcsdm.ecovault.dto.SalaryStatistics;
import com.tlcsdm.ecovault.security.SecurityUtils;
import com.tlcsdm.ecovault.service.SalaryService;
import jakarta.validation.Valid;
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
import java.util.List;

/**
 * 财务 - 工资数据管理接口。
 *
 * <p>
 * 工资数据管理属于财务管理模块，后续可在该模块下扩展消费数据管理等子模块。
 * </p>
 *
 * @author unknowIfGuestInDream
 */
@RestController
@RequestMapping("/api/finance/salaries")
public class SalaryController {

	private final SalaryService salaryService;

	public SalaryController(SalaryService salaryService) {
		this.salaryService = salaryService;
	}

	/**
	 * 查询工资记录列表。
	 * @param year 年份 (可空)
	 * @return 工资记录列表
	 */
	@GetMapping
	public ApiResponse<List<SalaryResponse>> list(@RequestParam(required = false) Integer year) {
		return ApiResponse.success(salaryService.list(SecurityUtils.getCurrentUserId(), year));
	}

	/**
	 * 统计分析。
	 * @param year 年份 (可空)
	 * @return 统计结果
	 */
	@GetMapping("/statistics")
	public ApiResponse<SalaryStatistics> statistics(@RequestParam(required = false) Integer year) {
		return ApiResponse.success(salaryService.statistics(SecurityUtils.getCurrentUserId(), year));
	}

	/**
	 * 录入或更新工资数据 (按年月唯一)。
	 * @param request 请求
	 * @return 保存结果
	 */
	@PostMapping
	@OperationLogRecord(module = "财务管理", operation = "录入工资数据")
	public ApiResponse<SalaryResponse> save(@Valid @RequestBody SalaryRequest request) {
		return ApiResponse.success(salaryService.save(SecurityUtils.getCurrentUserId(), request));
	}

	/**
	 * 更新指定工资记录。
	 * @param id 记录 ID
	 * @param request 请求
	 * @return 更新结果
	 */
	@PutMapping("/{id}")
	@OperationLogRecord(module = "财务管理", operation = "更新工资数据")
	public ApiResponse<SalaryResponse> update(@PathVariable Long id, @Valid @RequestBody SalaryRequest request) {
		return ApiResponse.success(salaryService.update(SecurityUtils.getCurrentUserId(), id, request));
	}

	/**
	 * 删除工资记录。
	 * @param id 记录 ID
	 * @return 删除结果
	 */
	@DeleteMapping("/{id}")
	@OperationLogRecord(module = "财务管理", operation = "删除工资数据")
	public ApiResponse<Void> delete(@PathVariable Long id) {
		salaryService.delete(SecurityUtils.getCurrentUserId(), id);
		return ApiResponse.success();
	}

	/**
	 * 导出工资数据为 CSV 文件。
	 * @param year 年份 (可空)
	 * @return CSV 文件
	 */
	@GetMapping("/export")
	@OperationLogRecord(module = "财务管理", operation = "导出工资数据")
	public ResponseEntity<byte[]> export(@RequestParam(required = false) Integer year) {
		String csv = salaryService.exportCsv(SecurityUtils.getCurrentUserId(), year);
		byte[] body = csv.getBytes(StandardCharsets.UTF_8);
		return ResponseEntity.ok()
			.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"salary.csv\"")
			.contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
			.body(body);
	}

}
