package com.tlcsdm.ecovault.service;

import com.tlcsdm.ecovault.entity.OperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
	 * @param pageable 分页参数
	 * @return 分页结果
	 */
	Page<OperationLog> query(Long enforcedUserId, String module, String keyword, Pageable pageable);

}
