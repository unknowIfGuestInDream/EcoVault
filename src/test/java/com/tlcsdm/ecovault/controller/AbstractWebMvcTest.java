package com.tlcsdm.ecovault.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tlcsdm.ecovault.entity.Role;
import com.tlcsdm.ecovault.entity.User;
import com.tlcsdm.ecovault.security.SecurityUser;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/**
 * Web 层集成测试基类，提供 MockMvc 与认证主体构建辅助方法。
 *
 * @author unknowIfGuestInDream
 */
@SpringBootTest
abstract class AbstractWebMvcTest {

	@Autowired
	protected WebApplicationContext webApplicationContext;

	protected final ObjectMapper objectMapper = new ObjectMapper();

	protected MockMvc mockMvc;

	@BeforeEach
	void initMockMvc() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
	}

	/**
	 * 构建包装指定用户的安全主体。
	 * @param id 用户 ID
	 * @param username 用户名
	 * @param role 角色
	 * @return 安全主体
	 */
	protected SecurityUser securityUser(Long id, String username, Role role) {
		User user = new User();
		user.setId(id);
		user.setUsername(username);
		user.setNickname(username);
		user.setRole(role);
		user.setEnabled(true);
		return new SecurityUser(user);
	}

	/**
	 * 构建携带指定安全主体的认证对象，供 MockMvc 认证后置处理器使用。
	 * @param principal 安全主体
	 * @return 认证对象
	 */
	protected Authentication authFor(SecurityUser principal) {
		return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
	}

}
