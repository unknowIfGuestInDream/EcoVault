package com.tlcsdm.ecovault.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * CSRF Cookie 渲染过滤器。
 *
 * <p>Spring Security 默认延迟加载 CSRF 令牌，导致 {@code XSRF-TOKEN} Cookie 不会主动下发。
 * 本过滤器在每次请求时主动读取一次令牌，触发 {@code CookieCsrfTokenRepository} 写出 Cookie，
 * 便于前端 JavaScript 读取并在后续请求中通过 {@code X-XSRF-TOKEN} 头回传，实现 CSRF 防护。</p>
 *
 * @author 梦里不知身是客
 */
public class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            // 主动获取令牌值，触发 Cookie 写出
            csrfToken.getToken();
        }
        filterChain.doFilter(request, response);
    }
}
