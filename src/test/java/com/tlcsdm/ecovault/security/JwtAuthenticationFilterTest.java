package com.tlcsdm.ecovault.security;

import com.tlcsdm.ecovault.entity.Role;
import com.tlcsdm.ecovault.entity.User;
import com.tlcsdm.ecovault.entity.UserSession;
import com.tlcsdm.ecovault.repository.UserSessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * JWT 认证过滤器单元测试。
 *
 * <p>
 * 覆盖令牌来源 (请求头 / Cookie / 缺失)、令牌与会话有效性、账户启用状态 等分支，确保单设备登录约束与认证注入逻辑正确。
 * </p>
 *
 * @author unknowIfGuestInDream
 */
class JwtAuthenticationFilterTest {

	private final JwtTokenProvider tokenProvider = new JwtTokenProvider("filter-test-secret", 7200000L);

	private CustomUserDetailsService userDetailsService;

	private UserSessionRepository sessionRepository;

	private JwtAuthenticationFilter filter;

	@BeforeEach
	void setUp() {
		userDetailsService = mock(CustomUserDetailsService.class);
		sessionRepository = mock(UserSessionRepository.class);
		filter = new JwtAuthenticationFilter(tokenProvider, userDetailsService, sessionRepository);
	}

	@AfterEach
	void clear() {
		SecurityContextHolder.clearContext();
	}

	private SecurityUser securityUser(boolean enabled) {
		User user = new User();
		user.setId(1L);
		user.setUsername("alice");
		user.setRole(Role.USER);
		user.setEnabled(enabled);
		return new SecurityUser(user);
	}

	private UserSession session(String jti, boolean active) {
		UserSession session = new UserSession();
		session.setJti(jti);
		session.setActive(active);
		return session;
	}

	@Test
	@DisplayName("无令牌时不设置认证并放行")
	void noToken() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = mock(FilterChain.class);

		filter.doFilterInternal(request, response, chain);

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
		verify(chain).doFilter(request, response);
	}

	@Test
	@DisplayName("请求头携带有效令牌且会话活跃时注入认证")
	void validBearerToken() throws Exception {
		String jti = tokenProvider.newJti();
		String token = tokenProvider.generateToken("alice", 1L, jti);
		when(sessionRepository.findByJti(jti)).thenReturn(Optional.of(session(jti, true)));
		when(userDetailsService.loadUserByUsername("alice")).thenReturn(securityUser(true));

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer " + token);
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = mock(FilterChain.class);

		filter.doFilterInternal(request, response, chain);

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
		assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("alice");
		verify(chain).doFilter(request, response);
	}

	@Test
	@DisplayName("Cookie 中有效令牌且会话活跃时注入认证")
	void validCookieToken() throws Exception {
		String jti = tokenProvider.newJti();
		String token = tokenProvider.generateToken("alice", 1L, jti);
		when(sessionRepository.findByJti(jti)).thenReturn(Optional.of(session(jti, true)));
		when(userDetailsService.loadUserByUsername("alice")).thenReturn(securityUser(true));

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setCookies(new Cookie(JwtAuthenticationFilter.TOKEN_COOKIE, token), new Cookie("OTHER", "x"));
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = mock(FilterChain.class);

		filter.doFilterInternal(request, response, chain);

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
	}

	@Test
	@DisplayName("无关 Cookie 不解析为令牌")
	void unrelatedCookieOnly() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setCookies(new Cookie("OTHER", "x"));
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = mock(FilterChain.class);

		filter.doFilterInternal(request, response, chain);

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	@Test
	@DisplayName("令牌无效 (解析失败) 时不注入认证")
	void invalidToken() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		String bogus = "Bearer " + "not.a.valid.jwt";
		request.addHeader("Authorization", bogus);
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = mock(FilterChain.class);

		filter.doFilterInternal(request, response, chain);

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
		verify(userDetailsService, never()).loadUserByUsername(anyString());
	}

	@Test
	@DisplayName("会话已失效时不注入认证")
	void inactiveSession() throws Exception {
		String jti = tokenProvider.newJti();
		String token = tokenProvider.generateToken("alice", 1L, jti);
		when(sessionRepository.findByJti(jti)).thenReturn(Optional.of(session(jti, false)));

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer " + token);
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = mock(FilterChain.class);

		filter.doFilterInternal(request, response, chain);

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
		verify(userDetailsService, never()).loadUserByUsername(anyString());
	}

	@Test
	@DisplayName("会话不存在时不注入认证")
	void sessionNotFound() throws Exception {
		String jti = tokenProvider.newJti();
		String token = tokenProvider.generateToken("alice", 1L, jti);
		when(sessionRepository.findByJti(jti)).thenReturn(Optional.empty());

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer " + token);
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = mock(FilterChain.class);

		filter.doFilterInternal(request, response, chain);

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	@Test
	@DisplayName("账户被禁用时不注入认证")
	void disabledUser() throws Exception {
		String jti = tokenProvider.newJti();
		String token = tokenProvider.generateToken("alice", 1L, jti);
		when(sessionRepository.findByJti(jti)).thenReturn(Optional.of(session(jti, true)));
		when(userDetailsService.loadUserByUsername("alice")).thenReturn(securityUser(false));

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer " + token);
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = mock(FilterChain.class);

		filter.doFilterInternal(request, response, chain);

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	@Test
	@DisplayName("已存在认证时跳过令牌解析")
	void skipWhenAlreadyAuthenticated() throws Exception {
		SecurityContextHolder.getContext()
			.setAuthentication(new UsernamePasswordAuthenticationToken("existing", null, null));

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization",
				"Bearer " + tokenProvider.generateToken("alice", 1L, tokenProvider.newJti()));
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = mock(FilterChain.class);

		filter.doFilterInternal(request, response, chain);

		assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("existing");
		verify(userDetailsService, never()).loadUserByUsername(anyString());
	}

	@Test
	@DisplayName("令牌缺失 jti 时会话判定为不活跃，不注入认证")
	void tokenWithoutJti() throws Exception {
		String token = tokenProvider.generateToken("alice", 1L, null);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer " + token);
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = mock(FilterChain.class);

		filter.doFilterInternal(request, response, chain);

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
		verify(sessionRepository, never()).findByJti(anyString());
		verify(userDetailsService, never()).loadUserByUsername(anyString());
	}

	@Test
	@DisplayName("Authorization 头非令牌方案时不解析令牌")
	void nonBearerAuthorizationHeader() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = mock(FilterChain.class);

		filter.doFilterInternal(request, response, chain);

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
		verify(userDetailsService, never()).loadUserByUsername(anyString());
	}

}
