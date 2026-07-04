package com.tlcsdm.ecovault.service;

import com.tlcsdm.ecovault.entity.OperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

/**
 * 操作日志服务。
 *
 * @author unknowIfGuestInDream
 */
public interface OperationLogService {

	/**
	 * 异步保存操作日志。
	 * @param log 日志实体
	 */
	void save(OperationLog log);

	/**
	 * 分页查询日志。
	 *
	 * <p>
	 * 普通用户只能查询自身日志 (由调用方将 {@code enforcedUserId} 设为其用户 ID)； 管理员可查询全部 (传入 {@code null})。
	 * </p>
	 * @param enforcedUserId 限定用户 ID (可空)
	 * @param module 模块 (可空)
	 * @param keyword 关键字 (可空)
	 * @param start 起始时间 (可空)
	 * @param end 结束时间 (可空)
	 * @param pageable 分页参数
	 * @return 分页结果
	 */
	Page<OperationLog> query(Long enforcedUserId, String module, String keyword, LocalDateTime start, LocalDateTime end,
			Pageable pageable);

	/**
	 * 根据 ID 查询日志详情。
	 * @param id 日志 ID
	 * @return 日志实体
	 */
	OperationLog getById(Long id);

	/**
	 * 更新日志的模块与操作描述 (仅管理员可用)。
	 * @param id 日志 ID
	 * @param module 模块
	 * @param operation 操作描述
	 * @return 更新后的日志
	 */
	OperationLog update(Long id, String module, String operation);

	/**
	 * 删除日志。
	 * @param id 日志 ID
	 */
	void delete(Long id);

}
