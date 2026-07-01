package com.tlcsdm.ecovault.controller;

import com.tlcsdm.ecovault.dto.ChangePasswordRequest;
import com.tlcsdm.ecovault.dto.LoginRequest;
import com.tlcsdm.ecovault.dto.RegisterRequest;
import com.tlcsdm.ecovault.dto.UpdateProfileRequest;
import com.tlcsdm.ecovault.entity.Role;
import com.tlcsdm.ecovault.entity.User;
import com.tlcsdm.ecovault.security.JwtAuthenticationFilter;
import com.tlcsdm.ecovault.security.SecurityUser;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 认证接口集成测试，覆盖登录发放 Cookie、注销、个人信息与密码修改， 并通过真实 JWT Cookie 流转验证认证过滤器。
 *
 * @author unknowIfGuestInDream
 */
class AuthControllerTest extends AbstractWebMvcTest {

	private Cookie login(String username, String password) throws Exception {
		LoginRequest request = new LoginRequest(username, password);
		Cookie cookie = mockMvc
			.perform(post("/api/auth/login").with(csrf())
				.header(HttpHeaders.USER_AGENT, "JUnit-Agent")
				.header("X-Forwarded-For", "203.0.113.9")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=7200")))
			.andExpect(jsonPath("$.data.token").isNotEmpty())
			.andReturn()
			.getResponse()
			.getCookie(JwtAuthenticationFilter.TOKEN_COOKIE);
		assertThat(cookie).isNotNull();
		return cookie;
	}

	private void createUser(String username) throws Exception {
		Authentication admin = authFor(securityUser(1L, "admin", Role.ADMIN));
		RegisterRequest request = new RegisterRequest(username, "Passw0rd!", "初始昵称", username + "@ecovault.com");
		mockMvc
			.perform(post("/api/admin/users").with(authentication(admin))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("默认管理员登录成功并下发令牌 Cookie")
	void loginSuccess() throws Exception {
		login("admin", "Admin@123");
	}

	@Test
	@DisplayName("密码错误登录返回 400")
	void loginWrongPassword() throws Exception {
		LoginRequest request = new LoginRequest("admin", "wrong-password");
		mockMvc
			.perform(post("/api/auth/login").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(400));
	}

	@Test
	@DisplayName("获取当前用户信息，昵称与邮箱为空时回退为空字符串")
	void meWithNullFields() throws Exception {
		User user = new User();
		user.setId(5000L);
		user.setUsername("nulluser");
		user.setNickname(null);
		user.setEmail(null);
		user.setRole(Role.USER);
		user.setEnabled(true);
		Authentication auth = authFor(new SecurityUser(user));

		mockMvc.perform(get("/api/auth/me").with(authentication(auth)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.username").value("nulluser"))
			.andExpect(jsonPath("$.data.nickname").value(""))
			.andExpect(jsonPath("$.data.email").value(""))
			.andExpect(jsonPath("$.data.role").value("USER"));
	}

	@Test
	@DisplayName("通过 JWT Cookie 完成信息查询、资料更新、改密与注销全流程")
	void cookieAuthenticatedFlow() throws Exception {
		createUser("authflow");
		Cookie cookie = login("authflow", "Passw0rd!");

		// 携带 Cookie 访问 /me，验证 JWT 认证过滤器
		mockMvc.perform(get("/api/auth/me").cookie(cookie))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.username").value("authflow"));

		// 更新个人信息
		UpdateProfileRequest profile = new UpdateProfileRequest("新昵称", "updated@ecovault.com");
		mockMvc
			.perform(put("/api/auth/profile").cookie(cookie)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(profile)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(0));

		// 修改密码
		ChangePasswordRequest change = new ChangePasswordRequest("Passw0rd!", "NewPass123");
		mockMvc
			.perform(put("/api/auth/password").cookie(cookie)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(change)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(0));

		// 改密后使用新密码重新登录并注销
		Cookie newCookie = login("authflow", "NewPass123");
		mockMvc.perform(post("/api/auth/logout").cookie(newCookie).with(csrf()))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));
	}

	@Test
	@DisplayName("无令牌注销也返回成功 (jti 解析为空)")
	void logoutWithoutToken() throws Exception {
		Authentication auth = authFor(securityUser(6000L, "logoutuser", Role.USER));
		mockMvc.perform(post("/api/auth/logout").with(authentication(auth)).with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(0));
	}

	@Test
	@DisplayName("使用 Authorization 头的令牌完成注销")
	void logoutWithBearerHeader() throws Exception {
		createUser("bearerlogout");
		Cookie cookie = login("bearerlogout", "Passw0rd!");
		mockMvc
			.perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer " + cookie.getValue())
				.with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(0));
	}

	@Test
	@DisplayName("Authorization 头非令牌方案时按无令牌处理并成功注销")
	void logoutWithNonBearerHeader() throws Exception {
		Authentication auth = authFor(securityUser(6100L, "nonbearer", Role.USER));
		mockMvc
			.perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz")
				.with(authentication(auth))
				.with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(0));
	}

	@Test
	@DisplayName("Cookie 中含无关 Cookie 时仍能定位令牌 Cookie 完成注销")
	void logoutWithTokenCookieAmongOthers() throws Exception {
		createUser("multicookie");
		Cookie cookie = login("multicookie", "Passw0rd!");
		mockMvc.perform(post("/api/auth/logout").cookie(new Cookie("OTHER", "x"), cookie).with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(0));
	}

	@Test
	@DisplayName("仅含无关 Cookie 时遍历后未找到令牌，按无令牌成功注销")
	void logoutWithOnlyUnrelatedCookie() throws Exception {
		Authentication auth = authFor(securityUser(6300L, "onlyother", Role.USER));
		mockMvc
			.perform(post("/api/auth/logout").cookie(new Cookie("OTHER", "x")).with(authentication(auth)).with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(0));
	}

	@Test
	@DisplayName("Authorization 头携带无效令牌时 jti 解析为空仍成功注销")
	void logoutWithInvalidBearerToken() throws Exception {
		Authentication auth = authFor(securityUser(6200L, "invalidtoken", Role.USER));
		mockMvc
			.perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer " + "not.a.valid.jwt")
				.with(authentication(auth))
				.with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(0));
	}

}
