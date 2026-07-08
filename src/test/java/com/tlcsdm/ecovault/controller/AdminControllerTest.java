package com.tlcsdm.ecovault.controller;

import com.tlcsdm.ecovault.dto.RegisterRequest;
import com.tlcsdm.ecovault.entity.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 管理后台接口集成测试，覆盖创建用户、用户列表、状态变更与构建信息。
 *
 * @author unknowIfGuestInDream
 */
class AdminControllerTest extends AbstractWebMvcTest {

	private Authentication admin() {
		return authFor(securityUser(1L, "admin", Role.ADMIN));
	}

	private long createUser(String username) throws Exception {
		RegisterRequest request = new RegisterRequest(username, "Passw0rd!", "测试用户", username + "@ecovault.com");
		String body = mockMvc
			.perform(post("/api/admin/users").with(authentication(admin()))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(0))
			.andExpect(jsonPath("$.data.username").value(username))
			.andReturn()
			.getResponse()
			.getContentAsString();
		return objectMapper.readTree(body).path("data").path("id").asLong();
	}

	@Test
	@DisplayName("创建用户、查询列表并切换启用状态")
	void createListAndToggleStatus() throws Exception {
		long id = createUser("adminmgmt1");

		mockMvc.perform(get("/api/admin/users").with(authentication(admin())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").isArray());

		// 禁用
		mockMvc
			.perform(put("/api/admin/users/{id}/status", id).param("enabled", "false")
				.with(authentication(admin()))
				.with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(0));

		// 启用
		mockMvc
			.perform(put("/api/admin/users/{id}/status", id).param("enabled", "true")
				.with(authentication(admin()))
				.with(csrf()))
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("创建重复用户名返回业务错误")
	void createDuplicate() throws Exception {
		createUser("dupadmin");
		RegisterRequest request = new RegisterRequest("dupadmin", "Passw0rd!", "重复", "d@ecovault.com");
		mockMvc
			.perform(post("/api/admin/users").with(authentication(admin()))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(400));
	}

	@Test
	@DisplayName("创建指定角色用户、编辑信息与删除")
	void createWithRoleUpdateAndDelete() throws Exception {
		// 创建 ADMIN 角色用户
		RegisterRequest request = new RegisterRequest("roleuser1", "Passw0rd!", "角色用户", "roleuser1@ecovault.com",
				"ADMIN");
		String body = mockMvc
			.perform(post("/api/admin/users").with(authentication(admin()))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(0))
			.andReturn()
			.getResponse()
			.getContentAsString();
		long id = objectMapper.readTree(body).path("data").path("id").asLong();

		// 编辑：改昵称、降级为 USER、禁用
		String update = "{\"nickname\":\"改名\",\"email\":\"new@ecovault.com\",\"role\":\"USER\",\"enabled\":false}";
		mockMvc
			.perform(put("/api/admin/users/{id}", id).with(authentication(admin()))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(update))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.nickname").value("改名"))
			.andExpect(jsonPath("$.data.role").value("USER"));

		// 删除
		mockMvc.perform(delete("/api/admin/users/{id}", id).with(authentication(admin())).with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(0));
	}

	@Test
	@DisplayName("查询角色矩阵并更新角色页面权限")
	void roleMatrixAndUpdate() throws Exception {
		// 矩阵仅包含可配置页面
		mockMvc.perform(get("/api/admin/roles").with(authentication(admin())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(0))
			.andExpect(jsonPath("$.data.pages").isArray())
			.andExpect(jsonPath("$.data.roles").isArray());

		// 更新 USER 角色权限：仅保留密码管理
		mockMvc
			.perform(put("/api/admin/roles/{role}/permissions", "USER").with(authentication(admin()))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"pages\":[\"passwords\"]}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(0));

		// 恢复默认权限，避免影响其他测试
		mockMvc
			.perform(put("/api/admin/roles/{role}/permissions", "USER").with(authentication(admin()))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"pages\":[\"passwords\",\"salary\",\"ledger\"]}"))
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("更新角色权限时角色非法返回业务错误")
	void updateRoleInvalid() throws Exception {
		mockMvc
			.perform(put("/api/admin/roles/{role}/permissions", "UNKNOWN").with(authentication(admin()))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"pages\":[]}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(400));
	}

	@Test
	@DisplayName("不允许修改 ADMIN 角色页面权限")
	void updateAdminRoleRejected() throws Exception {
		mockMvc
			.perform(put("/api/admin/roles/{role}/permissions", "ADMIN").with(authentication(admin()))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"pages\":[\"passwords\"]}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(400))
			.andExpect(jsonPath("$.message").value("ADMIN 角色默认拥有全部页面访问权限，不允许修改"));
	}

	@Test
	@DisplayName("获取构建信息 (开发环境返回占位版本与 Java 版本)")
	void buildInfo() throws Exception {
		mockMvc.perform(get("/api/admin/build-info").with(authentication(admin())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(0))
			.andExpect(jsonPath("$.data.javaVersion").exists())
			.andExpect(jsonPath("$.data.group").doesNotExist())
			.andExpect(jsonPath("$.data.artifact").doesNotExist())
			.andExpect(jsonPath("$.data.springBootVersion").doesNotExist());
	}

	@Test
	@DisplayName("获取 Actuator 端点概览仅返回可直接访问的基础路径")
	void actuatorEndpoints() throws Exception {
		mockMvc.perform(get("/api/admin/actuator-endpoints").with(authentication(admin())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(0))
			.andExpect(content().string(containsString("/actuator/health")))
			.andExpect(content().string(not(containsString("/actuator/env/{toMatch}"))))
			.andExpect(content().string(not(containsString("/actuator/loggers/{name}"))))
			.andExpect(content().string(not(containsString("/actuator/health/{*path}"))))
			.andExpect(content().string(not(containsString("/actuator/metrics/{requiredMetricName}"))));
	}

	@Test
	@DisplayName("普通用户访问管理接口返回 403")
	void forbiddenForUser() throws Exception {
		mockMvc.perform(get("/api/admin/users").with(authentication(authFor(securityUser(2L, "user", Role.USER)))))
			.andExpect(status().isForbidden());
	}

}
