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
 */
@Service
public class OperationLogServiceImpl implements OperationLogService {

	private final OperationLogRepository repository;

	public OperationLogServiceImpl(OperationLogRepository repository) {
		this.repository = repository;
	}

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

	@Override
	public OperationLog getById(Long id) {
		return repository.findById(id).orElseThrow(() -> new BusinessException("日志不存在"));
	}

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

	@Override
	public void delete(Long id) {
		OperationLog log = getById(id);
		repository.delete(log);
	}

}
