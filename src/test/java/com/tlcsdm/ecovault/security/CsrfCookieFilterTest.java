package com.tlcsdm.ecovault.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * CSRF Cookie 渲染过滤器单元测试。
 *
 * @author unknowIfGuestInDream
 */
class CsrfCookieFilterTest {

	private final CsrfCookieFilter filter = new CsrfCookieFilter();

	@Test
	@DisplayName("存在 CSRF 令牌时主动读取以触发 Cookie 写出并放行")
	void withCsrfToken() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		CsrfToken token = new DefaultCsrfToken("X-XSRF-TOKEN", "_csrf", "token-value");
		request.setAttribute(CsrfToken.class.getName(), token);
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = mock(FilterChain.class);

		filter.doFilterInternal(request, response, chain);

		verify(chain).doFilter(request, response);
	}

	@Test
	@DisplayName("无 CSRF 令牌时直接放行")
	void withoutCsrfToken() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = mock(FilterChain.class);

		filter.doFilterInternal(request, response, chain);

		verify(chain).doFilter(request, response);
	}

}
