package com.tlcsdm.ecovault.service;

import com.tlcsdm.ecovault.dto.SalaryRequest;
import com.tlcsdm.ecovault.dto.SalaryResponse;
import com.tlcsdm.ecovault.dto.SalaryStatistics;

import java.util.List;
import java.util.Map;

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
	 * 查询工资记录列表，支持年份区间筛选。
	 * @param userId 用户 ID
	 * @param startYear 起始年份（含，可空）
	 * @param endYear 结束年份（含，可空）
	 * @return 工资记录列表
	 */
	List<SalaryResponse> list(Long userId, Integer startYear, Integer endYear);

	/**
	 * 统计分析，支持年份区间筛选。
	 * @param userId 用户 ID
	 * @param startYear 起始年份（含，可空）
	 * @param endYear 结束年份（含，可空）
	 * @return 统计结果
	 */
	SalaryStatistics statistics(Long userId, Integer startYear, Integer endYear);

	/**
	 * 导出工资数据为 CSV。
	 * @param userId 用户 ID
	 * @param startYear 起始年份（含，可空）
	 * @param endYear 结束年份（含，可空）
	 * @return CSV 文本及推荐文件名，key 为 "csv" 与 "filename"
	 */
	Map<String, String> exportCsv(Long userId, Integer startYear, Integer endYear);

	/**
	 * 批量导入 CSV 格式工资数据（与导出格式一致）。
	 * @param userId 用户 ID
	 * @param csvContent CSV 文本内容（含表头）
	 * @return 成功导入条数
	 */
	int importCsv(Long userId, String csvContent);

}
