package com.tlcsdm.ecovault.controller;

import com.tlcsdm.ecovault.common.ApiResponse;
import com.tlcsdm.ecovault.service.AdminService;
import com.tlcsdm.ecovault.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
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

	private final Environment environment = mock(Environment.class);

	private WebEndpointsSupplier webEndpointsSupplier;

	private WebEndpointProperties webEndpointProperties;

	@SuppressWarnings("unchecked")
	private AdminController controllerWith(BuildProperties props) {
		ObjectProvider<BuildProperties> provider = mock(ObjectProvider.class);
		when(provider.getIfAvailable()).thenReturn(props);
		when(environment.getActiveProfiles()).thenReturn(new String[] { "prod" });
		return new AdminController(adminService, authService, rolePermissionService, provider, environment,
				webEndpointsSupplier, webEndpointProperties);
	}

	@BeforeEach
	void resetEndpointMocks() {
		webEndpointsSupplier = mock(WebEndpointsSupplier.class);
		webEndpointProperties = mock(WebEndpointProperties.class);
	}

	@Test
	@DisplayName("构建信息可用时返回完整版本与构建时间")
	void buildInfoAvailable() {
		Properties properties = new Properties();
		properties.setProperty("name", "EcoVault");
		properties.setProperty("version", "1.2.3");
		properties.setProperty("time", "1700000000000");
		AdminController controller = controllerWith(new BuildProperties(properties));

		ApiResponse<Map<String, Object>> response = controller.buildInfo();

		assertThat(response.getData()).containsEntry("name", "EcoVault").containsEntry("version", "1.2.3");
		assertThat(response.getData().get("buildTime")).isNotNull();
		assertThat(response.getData()).doesNotContainKeys("group", "artifact", "springBootVersion");
		assertThat(response.getData()).containsEntry("javaVersion", System.getProperty("java.version"));
		assertThat(response.getData()).containsEntry("javaVendor", System.getProperty("java.vendor"));
		assertThat(response.getData()).containsEntry("fileEncoding", System.getProperty("file.encoding"));
		assertThat(response.getData()).containsEntry("activeProfiles", "prod");
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

	@Test
	@DisplayName("Actuator 端点概览仅返回基础端点路径")
	void actuatorEndpoints() {
		ExposableWebEndpoint env = endpoint("env");
		ExposableWebEndpoint health = endpoint("health");
		ExposableWebEndpoint metrics = endpoint("metrics");
		when(webEndpointsSupplier.getEndpoints()).thenReturn(List.of(metrics, health, env));
		when(webEndpointProperties.getBasePath()).thenReturn("/actuator");
		AdminController controller = controllerWith(null);

		ApiResponse<List<Map<String, String>>> response = controller.actuatorEndpoints();

		assertThat(response.getData()).containsExactly(Map.of("name", "env", "path", "/actuator/env"),
				Map.of("name", "health", "path", "/actuator/health"),
				Map.of("name", "metrics", "path", "/actuator/metrics"));
	}

	@Test
	@DisplayName("Actuator 基础路径为 null 时端点路径不包含前缀")
	void actuatorEndpointsWithNullBasePath() {
		assertActuatorEndpointsWithoutBasePath(null);
	}

	@Test
	@DisplayName("Actuator 基础路径为空白时端点路径不包含前缀")
	void actuatorEndpointsWithBlankBasePath() {
		assertActuatorEndpointsWithoutBasePath("   ");
	}

	@Test
	@DisplayName("Actuator 基础路径为根路径时端点路径不包含前缀")
	void actuatorEndpointsWithRootBasePath() {
		assertActuatorEndpointsWithoutBasePath("/");
	}

	private void assertActuatorEndpointsWithoutBasePath(String basePath) {
		ExposableWebEndpoint health = endpoint("health");
		when(webEndpointsSupplier.getEndpoints()).thenReturn(List.of(health));
		when(webEndpointProperties.getBasePath()).thenReturn(basePath);
		AdminController controller = controllerWith(null);

		ApiResponse<List<Map<String, String>>> response = controller.actuatorEndpoints();

		assertThat(response.getCode()).isZero();
		assertThat(response.getData()).containsExactly(Map.of("name", "health", "path", "/health"));
	}

	private static ExposableWebEndpoint endpoint(String name) {
		ExposableWebEndpoint endpoint = mock(ExposableWebEndpoint.class);
		when(endpoint.getEndpointId()).thenReturn(EndpointId.of(name));
		when(endpoint.getRootPath()).thenReturn(name);
		return endpoint;
	}

}
