package com.tlcsdm.ecovault.service.impl;

import com.tlcsdm.ecovault.entity.OperationLog;
import com.tlcsdm.ecovault.repository.OperationLogRepository;
import com.tlcsdm.ecovault.service.OperationLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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
	public Page<OperationLog> query(Long enforcedUserId, String module, String keyword, Pageable pageable) {
		String normalizedModule = (module == null || module.isBlank()) ? null : module;
		String normalizedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
		return repository.search(enforcedUserId, normalizedModule, normalizedKeyword, pageable);
	}

}
