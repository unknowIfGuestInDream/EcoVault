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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 财务 - 工资数据管理接口。
 *
 * <p>
 * 工资数据管理属于财务管理模块，后续可在该模块下扩展消费数据管理等子模块。
 * </p>
 *
 * @author unknowIfGuestInDream
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/finance/salaries")
/**
 * 工资管理控制器，提供工资记录维护、统计分析与导出接口。 用于处理个人工资数据的查询与展示。
 *
 * @author unknowIfGuestInDream
 * @since 1.0.0
 */
public class SalaryController {

	private final SalaryService salaryService;

	/**
	 * 构造SalaryController实例并注入所需依赖。
	 * @param salaryService salaryService参数。
	 */
	public SalaryController(SalaryService salaryService) {
		this.salaryService = salaryService;
	}

	/**
	 * 查询工资记录列表，支持年份区间筛选。
	 * @param startYear 起始年份（含，可空）
	 * @param endYear 结束年份（含，可空）
	 * @return 工资记录列表
	 */
	@GetMapping
	public ApiResponse<List<SalaryResponse>> list(@RequestParam(required = false) Integer startYear,
			@RequestParam(required = false) Integer endYear) {
		return ApiResponse.success(salaryService.list(SecurityUtils.getCurrentUserId(), startYear, endYear));
	}

	/**
	 * 统计分析，支持年份区间筛选。
	 * @param startYear 起始年份（含，可空）
	 * @param endYear 结束年份（含，可空）
	 * @return 统计结果
	 */
	@GetMapping("/statistics")
	public ApiResponse<SalaryStatistics> statistics(@RequestParam(required = false) Integer startYear,
			@RequestParam(required = false) Integer endYear) {
		return ApiResponse.success(salaryService.statistics(SecurityUtils.getCurrentUserId(), startYear, endYear));
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
	 * 导出工资数据为 CSV 文件，支持年份区间筛选，文件名包含年份信息。
	 * @param startYear 起始年份（含，可空）
	 * @param endYear 结束年份（含，可空）
	 * @return CSV 文件
	 */
	@GetMapping("/export")
	@OperationLogRecord(module = "财务管理", operation = "导出工资数据")
	public ResponseEntity<byte[]> export(@RequestParam(required = false) Integer startYear,
			@RequestParam(required = false) Integer endYear) {
		Map<String, String> result = salaryService.exportCsv(SecurityUtils.getCurrentUserId(), startYear, endYear);
		byte[] body = result.get("csv").getBytes(StandardCharsets.UTF_8);
		String filename = result.get("filename");
		return ResponseEntity.ok()
			.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
			.contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
			.body(body);
	}

	/**
	 * 批量导入工资数据（CSV 格式，与导出格式一致）。
	 * @param file 上传的 CSV 文件
	 * @return 成功导入条数
	 */
	@PostMapping("/import")
	@OperationLogRecord(module = "财务管理", operation = "批量导入工资数据")
	public ApiResponse<Integer> importCsv(@RequestParam("file") MultipartFile file) throws IOException {
		if (file.isEmpty()) {
			return ApiResponse.error(400, "上传文件为空");
		}
		String content = new String(file.getBytes(), StandardCharsets.UTF_8);
		int count = salaryService.importCsv(SecurityUtils.getCurrentUserId(), content);
		return ApiResponse.success(count);
	}

}
