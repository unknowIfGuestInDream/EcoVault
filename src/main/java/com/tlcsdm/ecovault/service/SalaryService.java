package com.tlcsdm.ecovault.service;

import com.tlcsdm.ecovault.dto.SalaryRequest;
import com.tlcsdm.ecovault.dto.SalaryResponse;
import com.tlcsdm.ecovault.dto.SalaryStatistics;

import java.util.List;

/**
 * 财务 - 工资数据管理服务。
 *
 * @author unknowIfGuestInDream
 */
public interface SalaryService {

	/**
	 * 录入或更新工资数据 (按用户+年月唯一)。
	 * @param userId 用户 ID
	 * @param request 请求
	 * @return 保存后的数据
	 */
	SalaryResponse save(Long userId, SalaryRequest request);

	/**
	 * 更新指定工资记录。
	 * @param userId 用户 ID
	 * @param id 记录 ID
	 * @param request 请求
	 * @return 更新后的数据
	 */
	SalaryResponse update(Long userId, Long id, SalaryRequest request);

	/**
	 * 删除工资记录。
	 * @param userId 用户 ID
	 * @param id 记录 ID
	 */
	void delete(Long userId, Long id);

	/**
	 * 查询工资记录列表。
	 * @param userId 用户 ID
	 * @param year 年份 (可空，为空查询全部)
	 * @return 工资记录列表
	 */
	List<SalaryResponse> list(Long userId, Integer year);

	/**
	 * 统计分析。
	 * @param userId 用户 ID
	 * @param year 年份 (可空)
	 * @return 统计结果
	 */
	SalaryStatistics statistics(Long userId, Integer year);

	/**
	 * 导出工资数据为 CSV。
	 * @param userId 用户 ID
	 * @param year 年份 (可空)
	 * @return CSV 文本
	 */
	String exportCsv(Long userId, Integer year);

}
