package com.tlcsdm.ecovault.service;

import com.tlcsdm.ecovault.entity.OperationLog;
import com.tlcsdm.ecovault.repository.OperationLogRepository;
import com.tlcsdm.ecovault.service.impl.OperationLogServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 操作日志服务实现单元测试。
 *
 * @author unknowIfGuestInDream
 */
@ExtendWith(MockitoExtension.class)
class OperationLogServiceImplTest {

	@Mock
	private OperationLogRepository repository;

	@InjectMocks
	private OperationLogServiceImpl service;

	@Test
	@DisplayName("保存日志委托给仓储层")
	void save() {
		OperationLog log = new OperationLog();
		service.save(log);
		verify(repository).save(log);
	}

	@Test
	@DisplayName("查询时空白模块与关键字归一化为 null")
	void queryNormalizesBlank() {
		Pageable pageable = PageRequest.of(0, 20);
		Page<OperationLog> page = new PageImpl<>(List.of());
		when(repository.search(isNull(), isNull(), isNull(), eq(pageable))).thenReturn(page);

		Page<OperationLog> result = service.query(null, "   ", "  ", pageable);

		assertThat(result).isSameAs(page);
		verify(repository).search(isNull(), isNull(), isNull(), eq(pageable));
	}

	@Test
	@DisplayName("查询时保留有效模块并裁剪关键字空白")
	void queryTrimsKeyword() {
		Pageable pageable = PageRequest.of(0, 10);
		Page<OperationLog> page = new PageImpl<>(List.of(new OperationLog()));
		when(repository.search(eq(5L), eq("密码管理"), eq("github"), eq(pageable))).thenReturn(page);

		Page<OperationLog> result = service.query(5L, "密码管理", "  github  ", pageable);

		assertThat(result.getContent()).hasSize(1);
		ArgumentCaptor<String> keywordCaptor = ArgumentCaptor.forClass(String.class);
		verify(repository).search(eq(5L), eq("密码管理"), keywordCaptor.capture(), eq(pageable));
		assertThat(keywordCaptor.getValue()).isEqualTo("github");
	}

}
