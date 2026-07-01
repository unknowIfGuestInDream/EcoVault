package com.tlcsdm.ecovault.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 请求工具单元测试。
 *
 * @author unknowIfGuestInDream
 */
class WebUtilTest {

	@Test
	@DisplayName("请求为 null 时返回 unknown")
	void nullRequest() {
		assertThat(WebUtil.getClientIp(null)).isEqualTo("unknown");
	}

	@Test
	@DisplayName("X-Forwarded-For 多级代理取第一个 IP")
	void forwardedForMultiple() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getHeader("X-Forwarded-For")).thenReturn("1.1.1.1, 2.2.2.2, 3.3.3.3");
		assertThat(WebUtil.getClientIp(request)).isEqualTo("1.1.1.1");
	}

	@Test
	@DisplayName("X-Forwarded-For 单个 IP 直接返回")
	void forwardedForSingle() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getHeader("X-Forwarded-For")).thenReturn("9.9.9.9");
		assertThat(WebUtil.getClientIp(request)).isEqualTo("9.9.9.9");
	}

	@Test
	@DisplayName("X-Forwarded-For 为 unknown 时回退到 X-Real-IP")
	void fallbackToRealIp() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getHeader("X-Forwarded-For")).thenReturn("unknown");
		when(request.getHeader("X-Real-IP")).thenReturn("8.8.8.8");
		assertThat(WebUtil.getClientIp(request)).isEqualTo("8.8.8.8");
	}

	@Test
	@DisplayName("X-Forwarded-For 为空白时回退到 X-Real-IP")
	void blankForwardedForFallback() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getHeader("X-Forwarded-For")).thenReturn("   ");
		when(request.getHeader("X-Real-IP")).thenReturn("7.7.7.7");
		assertThat(WebUtil.getClientIp(request)).isEqualTo("7.7.7.7");
	}

	@Test
	@DisplayName("无代理头时使用 RemoteAddr")
	void remoteAddr() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getHeader("X-Forwarded-For")).thenReturn(null);
		when(request.getHeader("X-Real-IP")).thenReturn(null);
		when(request.getRemoteAddr()).thenReturn("127.0.0.1");
		assertThat(WebUtil.getClientIp(request)).isEqualTo("127.0.0.1");
	}

	@Test
	@DisplayName("X-Real-IP 为 unknown 时回退到 RemoteAddr")
	void realIpUnknownFallback() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getHeader("X-Forwarded-For")).thenReturn(null);
		when(request.getHeader("X-Real-IP")).thenReturn("unknown");
		when(request.getRemoteAddr()).thenReturn("192.168.0.1");
		assertThat(WebUtil.getClientIp(request)).isEqualTo("192.168.0.1");
	}

}
