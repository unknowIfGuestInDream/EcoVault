package com.tlcsdm.ecovault.controller;

import com.tlcsdm.ecovault.annotation.OperationLogRecord;
import com.tlcsdm.ecovault.common.ApiResponse;
import com.tlcsdm.ecovault.common.BusinessException;
import com.tlcsdm.ecovault.config.DateTimeConfig;
import com.tlcsdm.ecovault.dto.AdminUserResponse;
import com.tlcsdm.ecovault.dto.RegisterRequest;
import com.tlcsdm.ecovault.dto.RoleMatrixResponse;
import com.tlcsdm.ecovault.dto.UpdateRolePermissionRequest;
import com.tlcsdm.ecovault.dto.UpdateUserRequest;
import com.tlcsdm.ecovault.entity.Role;
import com.tlcsdm.ecovault.entity.User;
import com.tlcsdm.ecovault.service.AdminService;
import com.tlcsdm.ecovault.service.AuthService;
import com.tlcsdm.ecovault.service.RolePermissionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理后台接口 (仅管理员，路径受安全配置保护)。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

	private static final DateTimeFormatter BUILD_TIME_FORMATTER = DateTimeFormatter
		.ofPattern(DateTimeConfig.DATE_TIME_PATTERN);

	private final AdminService adminService;

	private final AuthService authService;

	private final RolePermissionService rolePermissionService;

	private final ObjectProvider<BuildProperties> buildProperties;

	private final Environment environment;

	private final WebEndpointsSupplier webEndpointsSupplier;

	private final WebEndpointProperties webEndpointProperties;

	public AdminController(AdminService adminService, AuthService authService,
			RolePermissionService rolePermissionService, ObjectProvider<BuildProperties> buildProperties,
			Environment environment, WebEndpointsSupplier webEndpointsSupplier,
			WebEndpointProperties webEndpointProperties) {
		this.adminService = adminService;
		this.authService = authService;
		this.rolePermissionService = rolePermissionService;
		this.buildProperties = buildProperties;
		this.environment = environment;
		this.webEndpointsSupplier = webEndpointsSupplier;
		this.webEndpointProperties = webEndpointProperties;
	}

	/**
	 * 由管理员创建普通用户。
	 * @param request 创建请求
	 * @return 创建结果
	 */
	@PostMapping("/users")
	@OperationLogRecord(module = "管理后台", operation = "创建用户")
	public ApiResponse<Map<String, Object>> createUser(@Valid @RequestBody RegisterRequest request) {
		User user = authService.register(request);
		return ApiResponse.success(Map.of("id", user.getId(), "username", user.getUsername()));
	}

	/**
	 * 查询全部用户。
	 * @return 用户列表
	 */
	@GetMapping("/users")
	public ApiResponse<List<AdminUserResponse>> listUsers() {
		return ApiResponse.success(adminService.listUsers());
	}

	/**
	 * 启用/禁用用户。
	 * @param id 用户 ID
	 * @param enabled 是否启用
	 * @return 操作结果
	 */
	@PutMapping("/users/{id}/status")
	@OperationLogRecord(module = "管理后台", operation = "修改用户状态")
	public ApiResponse<Void> setStatus(@PathVariable Long id, @RequestParam boolean enabled) {
		adminService.setUserEnabled(id, enabled);
		return ApiResponse.success();
	}

	/**
	 * 更新用户信息。
	 * @param id 用户 ID
	 * @param request 更新请求
	 * @return 更新后的用户信息
	 */
	@PutMapping("/users/{id}")
	@OperationLogRecord(module = "管理后台", operation = "更新用户")
	public ApiResponse<AdminUserResponse> updateUser(@PathVariable Long id,
			@Valid @RequestBody UpdateUserRequest request) {
		return ApiResponse.success(adminService.updateUser(id, request));
	}

	/**
	 * 删除用户。
	 * @param id 用户 ID
	 * @return 操作结果
	 */
	@DeleteMapping("/users/{id}")
	@OperationLogRecord(module = "管理后台", operation = "删除用户")
	public ApiResponse<Void> deleteUser(@PathVariable Long id) {
		adminService.deleteUser(id);
		return ApiResponse.success();
	}

	/**
	 * 查询角色权限矩阵。
	 * @return 角色与可访问页面矩阵
	 */
	@GetMapping("/roles")
	public ApiResponse<RoleMatrixResponse> roleMatrix() {
		return ApiResponse.success(rolePermissionService.getMatrix());
	}

	/**
	 * 更新指定角色的页面访问权限。
	 * @param role 角色名称
	 * @param request 权限请求
	 * @return 操作结果
	 */
	@PutMapping("/roles/{role}/permissions")
	@OperationLogRecord(module = "管理后台", operation = "更新角色权限")
	public ApiResponse<Void> updateRolePermissions(@PathVariable String role,
			@RequestBody UpdateRolePermissionRequest request) {
		Role parsed;
		try {
			parsed = Role.valueOf(role.trim().toUpperCase());
		}
		catch (IllegalArgumentException ex) {
			throw new BusinessException("角色不合法");
		}
		rolePermissionService.updatePermissions(parsed, request.pages());
		return ApiResponse.success();
	}

	/**
	 * 获取系统构建信息 (供管理员/开发者查看当前部署版本)。
	 * @return 构建信息
	 */
	@GetMapping("/build-info")
	public ApiResponse<Map<String, Object>> buildInfo() {
		Map<String, Object> info = new LinkedHashMap<>();
		BuildProperties props = buildProperties.getIfAvailable();
		if (props != null) {
			info.put("name", props.getName());
			info.put("version", props.getVersion());
			info.put("buildTime", props.getTime() == null ? null
					: BUILD_TIME_FORMATTER.format(props.getTime().atZone(DateTimeConfig.DEFAULT_ZONE_ID)));
		}
		else {
			info.put("version", "开发环境 (未生成构建信息)");
		}
		info.put("javaVersion", System.getProperty("java.version"));
		info.put("javaVendor", System.getProperty("java.vendor"));
		info.put("fileEncoding", System.getProperty("file.encoding"));
		info.put("activeProfiles", String.join(", ", environment.getActiveProfiles()));
		return ApiResponse.success(info);
	}

	/**
	 * 获取可直接访问的 Actuator 根端点列表。
	 * @return 端点名称与访问路径
	 */
	@GetMapping("/actuator-endpoints")
	public ApiResponse<List<Map<String, String>>> actuatorEndpoints() {
		String basePath = normalizeBasePath(webEndpointProperties.getBasePath());
		List<Map<String, String>> endpoints = webEndpointsSupplier.getEndpoints()
			.stream()
			.sorted(Comparator.comparing(ExposableWebEndpoint::getRootPath))
			.map(endpoint -> Map.of("name", endpoint.getEndpointId().toString(), "path",
					buildEndpointPath(basePath, endpoint.getRootPath())))
			.toList();
		return ApiResponse.success(endpoints);
	}

	private static String normalizeBasePath(String basePath) {
		if (basePath == null || basePath.isBlank() || "/".equals(basePath)) {
			return "";
		}
		return basePath.startsWith("/") ? basePath : "/" + basePath;
	}

	private static String buildEndpointPath(String basePath, String rootPath) {
		return basePath + (rootPath.startsWith("/") ? rootPath : "/" + rootPath);
	}

}
