package com.tlcsdm.ecovault.controller;

import com.tlcsdm.ecovault.entity.OperationLog;
import com.tlcsdm.ecovault.entity.Role;
import com.tlcsdm.ecovault.entity.User;
import com.tlcsdm.ecovault.security.SecurityUser;
import com.tlcsdm.ecovault.service.OperationLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 日志导出单元测试，重点覆盖创建时间为空与非空两种格式化分支。
 *
 * @author unknowIfGuestInDream
 */
class LogControllerExportTest {

	private final OperationLogService operationLogService = mock(OperationLogService.class);

	private final LogController controller = new LogController(operationLogService);

	@AfterEach
	void clear() {
		SecurityContextHolder.clearContext();
	}

	private void authenticateAdmin() {
		User user = new User();
		user.setId(1L);
		user.setUsername("admin");
		user.setRole(Role.ADMIN);
		user.setEnabled(true);
		SecurityUser principal = new SecurityUser(user);
		SecurityContextHolder.getContext()
			.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
	}

	@Test
	@DisplayName("导出时创建时间为空的日志行以空串占位，非空则格式化输出")
	void exportHandlesNullCreatedAt() {
		authenticateAdmin();

		OperationLog withTime = new OperationLog();
		withTime.setUsername("alice");
		withTime.setModule("登录");
		withTime.setCreatedAt(LocalDateTime.of(2025, 1, 2, 3, 4, 5));
		withTime.setDurationMs(12L);

		OperationLog withoutTime = new OperationLog();
		withoutTime.setUsername("bob");
		withoutTime.setModule("密码");
		withoutTime.setCreatedAt(null);
		withoutTime.setDurationMs(8L);

		when(operationLogService.query(isNull(), isNull(), isNull(), any(Pageable.class)))
			.thenReturn(new PageImpl<>(List.of(withTime, withoutTime)));

		ResponseEntity<byte[]> response = controller.export(null, null);

		String csv = new String(response.getBody(), StandardCharsets.UTF_8);
		assertThat(csv).contains("2025-01-02 03:04:05,alice,登录");
		// 创建时间为空的行以空串开头
		assertThat(csv).contains("\n,bob,密码");
	}

	@Test
	@DisplayName("普通用户导出仅限定自身日志")
	void exportForNormalUserScopesToSelf() {
		User user = new User();
		user.setId(42L);
		user.setUsername("user");
		user.setRole(Role.USER);
		user.setEnabled(true);
		SecurityUser principal = new SecurityUser(user);
		SecurityContextHolder.getContext()
			.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

		when(operationLogService.query(eq(42L), isNull(), isNull(), any(Pageable.class)))
			.thenReturn(new PageImpl<>(List.of()));

		ResponseEntity<byte[]> response = controller.export(null, null);

		assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
	}

}
