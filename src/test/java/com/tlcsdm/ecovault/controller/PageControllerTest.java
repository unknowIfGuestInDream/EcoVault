package com.tlcsdm.ecovault.controller;

import com.tlcsdm.ecovault.entity.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 页面路由控制器集成测试。
 *
 * @author unknowIfGuestInDream
 */
class PageControllerTest extends AbstractWebMvcTest {

	@Test
	@DisplayName("公开页面：首页与登录页可匿名访问")
	void publicPages() throws Exception {
		mockMvc.perform(get("/")).andExpect(status().isOk()).andExpect(content().string(containsString("EcoVault")));
		mockMvc.perform(get("/login")).andExpect(status().isOk());
	}

	@Test
	@DisplayName("受保护页面匿名访问跳转到登录页")
	void protectedPagesRedirect() throws Exception {
		for (String path : new String[] { "/dashboard", "/passwords", "/finance", "/finance/ledger", "/profile",
				"/admin/users", "/admin/logs", "/admin/roles" }) {
			mockMvc.perform(get(path))
				.andExpect(status().is3xxRedirection())
				.andExpect(header().string("Location", "/login"));
		}
	}

	@Test
	@DisplayName("认证用户可访问受保护页面")
	void authenticatedPages() throws Exception {
		var user = authFor(securityUser(1000L, "pageuser", Role.USER));
		for (String path : new String[] { "/dashboard", "/passwords", "/finance", "/finance/ledger", "/profile" }) {
			mockMvc.perform(get(path).with(authentication(user))).andExpect(status().isOk());
		}
	}

	@Test
	@DisplayName("后台页面仅管理员可访问，普通用户 403")
	void adminPage() throws Exception {
		var admin = authFor(securityUser(1L, "admin", Role.ADMIN));
		var user = authFor(securityUser(2L, "user", Role.USER));
		for (String path : new String[] { "/admin", "/admin/users", "/admin/logs", "/admin/roles" }) {
			mockMvc.perform(get(path).with(authentication(admin))).andExpect(status().isOk());
			mockMvc.perform(get(path).with(authentication(user))).andExpect(status().isForbidden());
		}
	}

	@Test
	@DisplayName("后台首页与用户管理页内容分离")
	void adminOverviewAndUsersPageSeparated() throws Exception {
		var admin = authFor(securityUser(1L, "admin", Role.ADMIN));
		mockMvc.perform(get("/admin").with(authentication(admin)))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("构建信息")))
			.andExpect(content().string(containsString("系统状态")))
			.andExpect(content().string(not(containsString("创建用户"))))
			.andExpect(content().string(not(containsString("用户列表"))));
		mockMvc.perform(get("/admin/users").with(authentication(admin)))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("创建用户")))
			.andExpect(content().string(containsString("用户列表")));
	}

	@Test
	@DisplayName("缺少页面权限时 guard 重定向到控制台")
	void configurablePageGuardRedirectsWhenPermissionMissing() throws Exception {
		var admin = authFor(securityUser(1L, "admin", Role.ADMIN));
		var user = authFor(securityUser(2L, "user", Role.USER));
		try {
			mockMvc
				.perform(put("/api/admin/roles/{role}/permissions", "USER").with(authentication(admin))
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"pages\":[\"passwords\",\"salary\"]}"))
				.andExpect(status().isOk());
			mockMvc.perform(get("/finance/ledger").with(authentication(user)))
				.andExpect(status().is3xxRedirection())
				.andExpect(header().string("Location", "/dashboard"));
		}
		finally {
			mockMvc
				.perform(put("/api/admin/roles/{role}/permissions", "USER").with(authentication(admin))
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"pages\":[\"passwords\",\"salary\",\"ledger\"]}"))
				.andExpect(status().isOk());
		}
	}

}
