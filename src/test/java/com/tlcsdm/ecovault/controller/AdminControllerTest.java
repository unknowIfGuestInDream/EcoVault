package com.tlcsdm.ecovault.controller;

import com.tlcsdm.ecovault.dto.RegisterRequest;
import com.tlcsdm.ecovault.entity.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
	@DisplayName("获取构建信息 (开发环境返回占位版本与 Java 版本)")
	void buildInfo() throws Exception {
		mockMvc.perform(get("/api/admin/build-info").with(authentication(admin())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(0))
			.andExpect(jsonPath("$.data.javaVersion").exists());
	}

	@Test
	@DisplayName("普通用户访问管理接口返回 403")
	void forbiddenForUser() throws Exception {
		mockMvc.perform(get("/api/admin/users").with(authentication(authFor(securityUser(2L, "user", Role.USER)))))
			.andExpect(status().isForbidden());
	}

}
