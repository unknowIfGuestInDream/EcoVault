package com.tlcsdm.ecovault.config;

import com.tlcsdm.ecovault.security.CsrfCookieFilter;
import com.tlcsdm.ecovault.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * Spring Security 安全配置。
 *
 * <p>
 * 核心策略：
 * </p>
 * <ul>
 * <li>无状态会话，基于 JWT 认证 (令牌存放于 HttpOnly + SameSite=Strict 的 Cookie，防御 XSS/CSRF)</li>
 * <li>密码使用 BCrypt 加密</li>
 * <li>启用 CSRF 保护 (CookieCsrfTokenRepository)，仅登录公开接口除外</li>
 * <li>actuator 与管理后台仅 ADMIN 角色可访问</li>
 * <li>方法级权限控制 ({@code @EnableMethodSecurity})</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
	}

	/**
	 * BCrypt 密码编码器。
	 * @return 密码编码器
	 */
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	/**
	 * 认证管理器。
	 * @param configuration 认证配置
	 * @return 认证管理器
	 * @throws Exception 获取失败时抛出
	 */
	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}

	/**
	 * 安全过滤链。
	 * @param http HttpSecurity
	 * @return 过滤链
	 * @throws Exception 配置失败时抛出
	 */
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
			// 使用普通请求处理器：前端将 Cookie 中的令牌原值通过 X-XSRF-TOKEN 头回传
			.csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
			// 公开接口 (登录) 无需 CSRF 令牌
			.ignoringRequestMatchers("/api/auth/login"))
			.authorizeHttpRequests(auth -> auth
				// 公开页面与静态资源
				.requestMatchers("/", "/login", "/error", "/css/**", "/js/**", "/images/**", "/webjars/**",
						"/favicon.ico")
				.permitAll()
				.requestMatchers("/api/auth/login")
				.permitAll()
				// actuator 与管理后台仅管理员
				.requestMatchers("/actuator/**", "/admin/**", "/api/admin/**", "/api/logs/**")
				.hasRole("ADMIN")
				.anyRequest()
				.authenticated())
			// 无状态：不使用 HttpSession
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			// 安全响应头：防御点击劫持、内容嗅探等
			.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()).contentTypeOptions(cto -> {
			}))
			.exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, authException) -> {
				String uri = request.getRequestURI();
				if (uri.startsWith("/api/")) {
					response.sendError(HttpStatus.UNAUTHORIZED.value(), "未认证或登录已失效");
				}
				else {
					response.sendRedirect(request.getContextPath() + "/login");
				}
			}))
			.formLogin(form -> form.disable())
			.httpBasic(basic -> basic.disable())
			.logout(logout -> logout.disable())
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
			// 强制下发 CSRF Cookie，供前端读取
			.addFilterAfter(new CsrfCookieFilter(), UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

}
