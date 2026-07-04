package com.tlcsdm.ecovault.service;

import com.tlcsdm.ecovault.common.BusinessException;
import com.tlcsdm.ecovault.dto.RoleMatrixResponse;
import com.tlcsdm.ecovault.entity.Role;
import com.tlcsdm.ecovault.entity.RolePermission;
import com.tlcsdm.ecovault.entity.User;
import com.tlcsdm.ecovault.repository.RolePermissionRepository;
import com.tlcsdm.ecovault.service.impl.RolePermissionServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 角色-页面权限服务单元测试。
 *
 * @author unknowIfGuestInDream
 */
@ExtendWith(MockitoExtension.class)
class RolePermissionServiceImplTest {

	@Mock
	private RolePermissionRepository repository;

	@InjectMocks
	private RolePermissionServiceImpl service;

	private User user(Role role) {
		User u = new User();
		u.setId(1L);
		u.setUsername("tester");
		u.setRole(role);
		return u;
	}

	@Test
	@DisplayName("初始化时为无权限的角色写入全部可配置页面")
	void initDefaultsSeedsMissingRoles() {
		when(repository.existsByRole(any())).thenReturn(false);

		service.initDefaults();

		// USER 与 ADMIN 各写入 3 个可配置页面 (passwords/salary/ledger)
		verify(repository, times(6)).save(any(RolePermission.class));
	}

	@Test
	@DisplayName("已存在权限的角色不重复初始化")
	void initDefaultsSkipsExisting() {
		when(repository.existsByRole(any())).thenReturn(true);

		service.initDefaults();

		verify(repository, never()).save(any());
	}

	@Test
	@DisplayName("角色矩阵仅包含可配置页面且返回各角色授权")
	void getMatrixReturnsConfigurablePages() {
		when(repository.findByRole(Role.USER))
			.thenReturn(List.of(new RolePermission(Role.USER, "passwords"), new RolePermission(Role.USER, "salary")));
		when(repository.findByRole(Role.ADMIN)).thenReturn(List.of(new RolePermission(Role.ADMIN, "ledger")));

		RoleMatrixResponse matrix = service.getMatrix();

		assertThat(matrix.pages()).extracting(RoleMatrixResponse.PageInfo::key)
			.containsExactlyInAnyOrder("passwords", "salary", "ledger");
		assertThat(matrix.roles()).anySatisfy(view -> {
			if ("USER".equals(view.role())) {
				assertThat(view.allowedPages()).containsExactlyInAnyOrder("passwords", "salary");
			}
		});
	}

	@Test
	@DisplayName("更新权限会先清空再写入合法页面")
	void updatePermissionsReplaces() {
		service.updatePermissions(Role.USER, List.of("passwords", " ", "ledger"));

		verify(repository).deleteByRole(Role.USER);
		verify(repository, times(2)).save(any(RolePermission.class));
	}

	@Test
	@DisplayName("更新权限包含非可配置页面应抛出异常")
	void updatePermissionsRejectsIllegalPage() {
		assertThatThrownBy(() -> service.updatePermissions(Role.USER, List.of("users")))
			.isInstanceOf(BusinessException.class);
	}

	@Test
	@DisplayName("ADMIN 角色权限固定开放，不允许修改")
	void updatePermissionsRejectsAdminRole() {
		assertThatThrownBy(() -> service.updatePermissions(Role.ADMIN, List.of("passwords")))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining("ADMIN 角色");
		verifyNoInteractions(repository);
	}

	@Test
	@DisplayName("管理员可访问全部页面")
	void adminAccessesEverything() {
		Set<String> keys = service.accessiblePageKeys(user(Role.ADMIN));
		assertThat(keys).contains("dashboard", "profile", "passwords", "salary", "ledger", "users", "logs", "roles");
		assertThat(service.canAccessPath(user(Role.ADMIN), "/admin/logs")).isTrue();
	}

	@Test
	@DisplayName("普通用户不可访问后台页面，可访问已授权的可配置页面")
	void userAccessRespectsPermissions() {
		lenient().when(repository.findByRole(Role.USER))
			.thenReturn(List.of(new RolePermission(Role.USER, "passwords"), new RolePermission(Role.USER, "ledger")));

		User user = user(Role.USER);
		Set<String> keys = service.accessiblePageKeys(user);
		assertThat(keys).contains("dashboard", "profile", "passwords", "ledger");
		assertThat(keys).doesNotContain("salary", "users", "logs", "roles");

		assertThat(service.canAccessPath(user, "/admin/logs")).isFalse();
		assertThat(service.canAccessPath(user, "/finance/ledger")).isTrue();
		assertThat(service.canAccessPath(user, "/finance")).isFalse();
		// 未受控页面 (控制台) 对所有登录用户开放
		assertThat(service.canAccessPath(user, "/dashboard")).isTrue();
	}

	@Test
	@DisplayName("未知路径默认放行")
	void unknownPathAllowed() {
		assertThat(service.canAccessPath(user(Role.USER), "/some/other/path")).isTrue();
	}

}
