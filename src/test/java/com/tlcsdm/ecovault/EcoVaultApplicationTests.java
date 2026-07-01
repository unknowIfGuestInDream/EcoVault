package com.tlcsdm.ecovault;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tlcsdm.ecovault.dto.LoginRequest;
import com.tlcsdm.ecovault.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 应用集成测试：验证上下文加载、管理员创建用户、登录流程与 RBAC 权限控制。
 */
@SpringBootTest
class EcoVaultApplicationTests {

	private static final String TRUSTED_ORIGIN = "https://eco.tlcsdm.com";

	private static final String MODERN_BROWSER = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
			+ "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36";

	@Autowired
	private WebApplicationContext webApplicationContext;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
	}

	@Test
	@DisplayName("Spring 上下文正常加载")
	void contextLoads() {
	}

	@Test
	@DisplayName("管理员创建用户后可成功登录并返回令牌")
	@WithMockUser(username = "admin", roles = "ADMIN")
	void adminCreateUserThenLogin() throws Exception {
		RegisterRequest request = new RegisterRequest("ituser", "Passw0rd!", "集成用户", "it@ecovault.com");
		mockMvc
			.perform(browserRequest(post("/api/admin/users")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(0));

		LoginRequest login = new LoginRequest("ituser", "Passw0rd!");
		mockMvc
			.perform(browserRequest(post("/api/auth/login")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(login)))
			.andExpect(status().isOk())
			.andExpect(header().string("Set-Cookie", containsString("Max-Age=7200")))
			.andExpect(jsonPath("$.code").value(0))
			.andExpect(jsonPath("$.data.token").isNotEmpty());
	}

	@Test
	@DisplayName("匿名用户无法调用外部注册接口")
	void publicRegisterEndpointUnavailable() throws Exception {
		RegisterRequest request = new RegisterRequest("ghost", "Passw0rd!", "匿名", "ghost@ecovault.com");
		mockMvc
			.perform(browserRequest(post("/api/auth/register")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("匿名用户访问旧注册页将跳转到登录页")
	void registerPageRedirectsToLogin() throws Exception {
		mockMvc.perform(get("/register"))
			.andExpect(status().is3xxRedirection())
			.andExpect(header().string("Location", "/login"));
	}

	@Test
	@DisplayName("普通用户访问管理接口返回 403")
	@WithMockUser(username = "user", roles = "USER")
	void userForbiddenOnAdminApi() throws Exception {
		mockMvc.perform(browserRequest(get("/api/admin/users"))).andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("管理员可访问管理接口")
	@WithMockUser(username = "admin", roles = "ADMIN")
	void adminAllowedOnAdminApi() throws Exception {
		mockMvc.perform(browserRequest(get("/api/admin/users")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(0));
	}

	@Test
	@DisplayName("未认证访问受保护接口返回 401")
	void unauthenticatedReturns401() throws Exception {
		mockMvc.perform(browserRequest(get("/api/passwords"))).andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("受信任来源的预检请求返回 CORS 响应头")
	void trustedOriginPreflightAllowed() throws Exception {
		mockMvc
			.perform(options("/api/passwords").header(HttpHeaders.ORIGIN, TRUSTED_ORIGIN)
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "X-XSRF-TOKEN,Content-Type"))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, TRUSTED_ORIGIN));
	}

	@Test
	@DisplayName("非白名单来源访问 API 返回 403")
	void untrustedOriginRejected() throws Exception {
		mockMvc
			.perform(get("/api/passwords").header(HttpHeaders.ORIGIN, "https://evil.example")
				.header(HttpHeaders.USER_AGENT, MODERN_BROWSER))
			.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("缺少 User-Agent 的 API 请求返回 403")
	void missingUserAgentRejected() throws Exception {
		mockMvc.perform(get("/api/passwords").header(HttpHeaders.ORIGIN, TRUSTED_ORIGIN))
			.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("浏览器版本过低的 API 请求返回 403")
	void outdatedUserAgentRejected() throws Exception {
		mockMvc
			.perform(get("/api/passwords").header(HttpHeaders.ORIGIN, TRUSTED_ORIGIN)
				.header(HttpHeaders.USER_AGENT,
						"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
								+ "(KHTML, like Gecko) Chrome/90.0.4430.212 Safari/537.36"))
			.andExpect(status().isForbidden());
	}

	private MockHttpServletRequestBuilder browserRequest(MockHttpServletRequestBuilder builder) {
		return builder.header(HttpHeaders.ORIGIN, TRUSTED_ORIGIN).header(HttpHeaders.USER_AGENT, MODERN_BROWSER);
	}

}
