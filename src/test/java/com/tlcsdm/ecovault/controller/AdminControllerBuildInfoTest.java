package com.tlcsdm.ecovault.controller;

import com.tlcsdm.ecovault.common.ApiResponse;
import com.tlcsdm.ecovault.service.AdminService;
import com.tlcsdm.ecovault.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;

import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 构建信息接口单元测试。
 *
 * <p>
 * 直接构造控制器并模拟 {@link BuildProperties} 提供者，覆盖「构建信息可用」「构建时间为空」 与「开发环境无构建信息」三种分支。
 * </p>
 *
 * @author unknowIfGuestInDream
 */
class AdminControllerBuildInfoTest {

	private final AdminService adminService = mock(AdminService.class);

	private final AuthService authService = mock(AuthService.class);

	private final com.tlcsdm.ecovault.service.RolePermissionService rolePermissionService = mock(
			com.tlcsdm.ecovault.service.RolePermissionService.class);

	@SuppressWarnings("unchecked")
	private AdminController controllerWith(BuildProperties props) {
		ObjectProvider<BuildProperties> provider = mock(ObjectProvider.class);
		when(provider.getIfAvailable()).thenReturn(props);
		return new AdminController(adminService, authService, rolePermissionService, provider);
	}

	@Test
	@DisplayName("构建信息可用时返回完整版本与构建时间")
	void buildInfoAvailable() {
		Properties properties = new Properties();
		properties.setProperty("group", "com.tlcsdm");
		properties.setProperty("artifact", "ecovault");
		properties.setProperty("name", "EcoVault");
		properties.setProperty("version", "1.2.3");
		properties.setProperty("time", "1700000000000");
		AdminController controller = controllerWith(new BuildProperties(properties));

		ApiResponse<Map<String, Object>> response = controller.buildInfo();

		assertThat(response.getData()).containsEntry("version", "1.2.3").containsEntry("artifact", "ecovault");
		assertThat(response.getData().get("buildTime")).isNotNull();
		assertThat(response.getData()).containsKey("javaVersion");
	}

	@Test
	@DisplayName("构建时间缺失时 buildTime 为 null")
	void buildInfoWithoutTime() {
		Properties properties = new Properties();
		properties.setProperty("version", "2.0.0");
		AdminController controller = controllerWith(new BuildProperties(properties));

		ApiResponse<Map<String, Object>> response = controller.buildInfo();

		assertThat(response.getData()).containsEntry("version", "2.0.0");
		assertThat(response.getData().get("buildTime")).isNull();
	}

	@Test
	@DisplayName("开发环境无构建信息时返回占位版本")
	void buildInfoDevelopmentFallback() {
		AdminController controller = controllerWith(null);

		ApiResponse<Map<String, Object>> response = controller.buildInfo();

		assertThat(response.getData()).containsEntry("version", "开发环境 (未生成构建信息)");
		assertThat(response.getData()).containsKey("javaVersion");
	}

}
