package com.tlcsdm.ecovault.security;

import com.tlcsdm.ecovault.entity.UserSession;
import com.tlcsdm.ecovault.repository.UserSessionRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * JWT 认证过滤器。
 *
 * <p>令牌可来源于两处 (二者其一即可)：请求头 {@code Authorization} 的 {@code BEARER_PREFIX}
 * 认证方案，或名为 {@code ECOVAULT_TOKEN} 的 Cookie；校验签名与有效期后，
 * 进一步校验令牌所属会话仍处于活跃状态 (实现单设备登录约束)。</p>
 *
 * @author unknowIfGuestInDream
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** 存放令牌的 Cookie 名 */
    public static final String TOKEN_COOKIE = "ECOVAULT_TOKEN";

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;

    private final CustomUserDetailsService userDetailsService;

    private final UserSessionRepository sessionRepository;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider,
                                   CustomUserDetailsService userDetailsService,
                                   UserSessionRepository sessionRepository) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
        this.sessionRepository = sessionRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            Claims claims = tokenProvider.parse(token);
            if (claims != null && isSessionActive(claims.getId())) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(claims.getSubject());
                if (userDetails.isEnabled()) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    /**
     * 校验会话是否处于活跃状态。
     *
     * @param jti 会话唯一标识
     * @return 是否活跃
     */
    private boolean isSessionActive(String jti) {
        if (jti == null) {
            return false;
        }
        Optional<UserSession> session = sessionRepository.findByJti(jti);
        return session.isPresent() && session.get().isActive();
    }

    /**
     * 从请求头或 Cookie 中提取令牌。
     *
     * @param request HTTP 请求
     * @return 令牌字符串；不存在返回 null
     */
    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (TOKEN_COOKIE.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
