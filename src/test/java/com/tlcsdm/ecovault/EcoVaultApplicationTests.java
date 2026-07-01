package com.tlcsdm.ecovault;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tlcsdm.ecovault.dto.LoginRequest;
import com.tlcsdm.ecovault.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Arrays;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 应用集成测试：验证上下文加载、管理员创建用户、登录流程与 RBAC 权限控制。
 */
@SpringBootTest
class EcoVaultApplicationTests {

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
	@DisplayName("启动 Banner 资源存在且包含应用名称")
	void bannerResourceExists() throws Exception {
		ClassPathResource banner = new ClassPathResource("banner.txt");
		assertTrue(banner.exists());
		String content = banner.getContentAsString(StandardCharsets.UTF_8);
		assertTrue(content.contains("EcoVault"));
	}

	@Test
	@DisplayName("首页包含 favicon 引用")
	void indexContainsFaviconLink() throws Exception {
		mockMvc.perform(get("/"))
			.andExpect(status().isOk())
			.andExpect(header().string("Content-Type", containsString("text/html")))
			.andExpect(content().string(containsString("<link rel=\"icon\" href=\"/favicon.ico\"")))
			.andExpect(content().string(containsString("<meta name=\"renderer\" content=\"webkit\"/>")))
			.andExpect(content().string(containsString("<meta name=\"google\" content=\"notranslate\"/>")));
	}

	@Test
	@DisplayName("favicon 资源可直接访问")
	void faviconIsServed() throws Exception {
		byte[] content = mockMvc.perform(get("/favicon.ico"))
			.andExpect(status().isOk())
			.andExpect(header().string("Content-Type", containsString("image")))
			.andReturn()
			.getResponse()
			.getContentAsByteArray();
		assertTrue(content.length > 6);
		assertArrayEquals(new byte[] { 0, 0, 1, 0 }, Arrays.copyOf(content, 4));
	}

	@Test
	@DisplayName("管理员创建用户后可成功登录并返回令牌")
	@WithMockUser(username = "admin", roles = "ADMIN")
	void adminCreateUserThenLogin() throws Exception {
		RegisterRequest request = new RegisterRequest("ituser", "Passw0rd!", "集成用户", "it@ecovault.com");
		mockMvc
			.perform(post("/api/admin/users").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(0));

		LoginRequest login = new LoginRequest("ituser", "Passw0rd!");
		mockMvc
			.perform(post("/api/auth/login").with(csrf())
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
			.perform(post("/api/auth/register").with(csrf())
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
		mockMvc.perform(get("/api/admin/users")).andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("管理员可访问管理接口")
	@WithMockUser(username = "admin", roles = "ADMIN")
	void adminAllowedOnAdminApi() throws Exception {
		mockMvc.perform(get("/api/admin/users")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));
	}

	@Test
	@DisplayName("未认证访问受保护接口返回 401")
	void unauthenticatedReturns401() throws Exception {
		mockMvc.perform(get("/api/passwords")).andExpect(status().isUnauthorized());
	}

}
