package com.tlcsdm.ecovault.controller;

import com.tlcsdm.ecovault.entity.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

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
		for (String path : new String[] { "/dashboard", "/passwords", "/finance", "/logs", "/profile" }) {
			mockMvc.perform(get(path))
				.andExpect(status().is3xxRedirection())
				.andExpect(header().string("Location", "/login"));
		}
	}

	@Test
	@DisplayName("认证用户可访问受保护页面")
	void authenticatedPages() throws Exception {
		var user = authFor(securityUser(1000L, "pageuser", Role.USER));
		for (String path : new String[] { "/dashboard", "/passwords", "/finance", "/logs", "/profile" }) {
			mockMvc.perform(get(path).with(authentication(user))).andExpect(status().isOk());
		}
	}

	@Test
	@DisplayName("管理后台页仅管理员可访问，普通用户 403")
	void adminPage() throws Exception {
		mockMvc.perform(get("/admin").with(authentication(authFor(securityUser(1L, "admin", Role.ADMIN)))))
			.andExpect(status().isOk());
		mockMvc.perform(get("/admin").with(authentication(authFor(securityUser(2L, "user", Role.USER)))))
			.andExpect(status().isForbidden());
	}

}
