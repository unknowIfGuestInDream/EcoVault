package com.tlcsdm.ecovault.service;

import com.tlcsdm.ecovault.dto.LedgerRequest;
import com.tlcsdm.ecovault.dto.LedgerResponse;
import com.tlcsdm.ecovault.dto.LedgerStatistics;

import java.time.LocalDate;
import java.util.List;

/**
 * 财务 - 收入支出管理服务。
 *
 * @author unknowIfGuestInDream
 */
public interface LedgerService {

	/**
	 * 新增一条收支记录。
	 * @param userId 用户 ID
	 * @param request 请求
	 * @return 保存结果
	 */
	LedgerResponse create(Long userId, LedgerRequest request);

	/**
	 * 更新指定收支记录。
	 * @param userId 用户 ID
	 * @param id 记录 ID
	 * @param request 请求
	 * @return 更新结果
	 */
	LedgerResponse update(Long userId, Long id, LedgerRequest request);

	/**
	 * 删除收支记录。
	 * @param userId 用户 ID
	 * @param id 记录 ID
	 */
	void delete(Long userId, Long id);

	/**
	 * 按条件查询收支记录。
	 * @param userId 用户 ID
	 * @param type 收支类型 (可空)
	 * @param start 起始日期 (可空)
	 * @param end 结束日期 (可空)
	 * @param tag 标签 (可空)
	 * @return 记录列表
	 */
	List<LedgerResponse> list(Long userId, String type, LocalDate start, LocalDate end, String tag);

	/**
	 * 收支统计分析。
	 * @param userId 用户 ID
	 * @param type 收支类型 (可空)
	 * @param start 起始日期 (可空)
	 * @param end 结束日期 (可空)
	 * @param tag 标签 (可空)
	 * @return 统计结果
	 */
	LedgerStatistics statistics(Long userId, String type, LocalDate start, LocalDate end, String tag);

	/**
	 * 导出收支记录为 CSV。
	 * @param userId 用户 ID
	 * @param type 收支类型 (可空)
	 * @param start 起始日期 (可空)
	 * @param end 结束日期 (可空)
	 * @param tag 标签 (可空)
	 * @return CSV 内容
	 */
	String exportCsv(Long userId, String type, LocalDate start, LocalDate end, String tag);

}
