package com.tlcsdm.ecovault.service.impl;

import com.tlcsdm.ecovault.common.BusinessException;
import com.tlcsdm.ecovault.entity.OperationLog;
import com.tlcsdm.ecovault.repository.OperationLogRepository;
import com.tlcsdm.ecovault.service.OperationLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 操作日志服务实现。
 *
 * @author unknowIfGuestInDream
 * @since 1.0.0
 * @see OperationLogService
 */
@Service
/**
 * 操作日志服务实现类，负责日志记录查询、更新与审计支持。 用于保障关键操作的可追溯性。
 *
 * @author unknowIfGuestInDream
 * @since 1.0.0
 * @see OperationLogService
 */
public class OperationLogServiceImpl implements OperationLogService {

	private final OperationLogRepository repository;

	/**
	 * 构造OperationLogServiceImpl实例并注入所需依赖。
	 * @param repository repository参数。
	 */
	public OperationLogServiceImpl(OperationLogRepository repository) {
		this.repository = repository;
	}

	/**
	 * 保存业务数据。
	 * @param log log参数。
	 */
	@Override
	public void save(OperationLog log) {
		repository.save(log);
	}

	@Override
	public Page<OperationLog> query(Long enforcedUserId, String module, String keyword, LocalDateTime start,
			LocalDateTime end, Pageable pageable) {
		String normalizedModule = (module == null || module.isBlank()) ? null : module;
		String normalizedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
		return repository.search(enforcedUserId, normalizedModule, normalizedKeyword, start, end, pageable);
	}

	/**
	 * 获取相关属性值。
	 * @param id 主键或记录编号。
	 * @return 方法执行结果。
	 */
	@Override
	public OperationLog getById(Long id) {
		return repository.findById(id).orElseThrow(() -> new BusinessException("日志不存在"));
	}

	/**
	 * 更新已有业务数据。
	 * @param id 主键或记录编号。
	 * @param module module参数。
	 * @param operation operation参数。
	 * @return 更新后的业务结果。
	 */
	@Override
	public OperationLog update(Long id, String module, String operation) {
		OperationLog log = getById(id);
		if (module != null && !module.isBlank()) {
			log.setModule(module.trim());
		}
		if (operation != null && !operation.isBlank()) {
			log.setOperation(operation.trim());
		}
		return repository.save(log);
	}

	/**
	 * 删除指定业务数据。
	 * @param id 主键或记录编号。
	 */
	@Override
	public void delete(Long id) {
		OperationLog log = getById(id);
		repository.delete(log);
	}

}
