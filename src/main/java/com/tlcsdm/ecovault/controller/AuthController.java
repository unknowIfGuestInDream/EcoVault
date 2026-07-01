package com.tlcsdm.ecovault.controller;

import com.tlcsdm.ecovault.annotation.OperationLogRecord;
import com.tlcsdm.ecovault.common.ApiResponse;
import com.tlcsdm.ecovault.dto.ChangePasswordRequest;
import com.tlcsdm.ecovault.dto.LoginRequest;
import com.tlcsdm.ecovault.dto.LoginResponse;
import com.tlcsdm.ecovault.dto.UpdateProfileRequest;
import com.tlcsdm.ecovault.entity.User;
import com.tlcsdm.ecovault.security.JwtAuthenticationFilter;
import com.tlcsdm.ecovault.security.JwtTokenProvider;
import com.tlcsdm.ecovault.security.SecurityUtils;
import com.tlcsdm.ecovault.service.AuthService;
import com.tlcsdm.ecovault.utils.WebUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

/**
 * 认证与用户信息接口。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	private final JwtTokenProvider tokenProvider;

	public AuthController(AuthService authService, JwtTokenProvider tokenProvider) {
		this.authService = authService;
		this.tokenProvider = tokenProvider;
	}

	/**
	 * 用户登录，成功后将 JWT 写入 HttpOnly + SameSite=Strict 的 Cookie。
	 * @param request 登录请求
	 * @param httpRequest HTTP 请求
	 * @return 登录响应
	 */
	@PostMapping("/login")
	@OperationLogRecord(module = "用户管理", operation = "用户登录")
	public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request,
			HttpServletRequest httpRequest) {
		String deviceInfo = httpRequest.getHeader(HttpHeaders.USER_AGENT);
		String ip = WebUtil.getClientIp(httpRequest);
		LoginResponse response = authService.login(request, deviceInfo, ip);

		ResponseCookie cookie = buildTokenCookie(response.token(), Duration.ofMillis(tokenProvider.getExpirationMs()));
		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, cookie.toString())
			.body(ApiResponse.success(response));
	}

	/**
	 * 注销登录，清除令牌 Cookie 并失效当前会话。
	 * @param httpRequest HTTP 请求
	 * @return 注销结果
	 */
	@PostMapping("/logout")
	@OperationLogRecord(module = "用户管理", operation = "用户注销")
	public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest httpRequest) {
		String jti = resolveJti(httpRequest);
		authService.logout(jti);
		ResponseCookie cookie = buildTokenCookie("", Duration.ZERO);
		return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body(ApiResponse.success());
	}

	/**
	 * 获取当前登录用户信息。
	 * @return 用户信息
	 */
	@GetMapping("/me")
	public ApiResponse<Map<String, Object>> me() {
		User user = SecurityUtils.getCurrentUser().getUser();
		return ApiResponse.success(
				Map.of("username", user.getUsername(), "nickname", user.getNickname() == null ? "" : user.getNickname(),
						"email", user.getEmail() == null ? "" : user.getEmail(), "role", user.getRole().name()));
	}

	/**
	 * 修改个人信息。
	 * @param request 修改请求
	 * @return 修改结果
	 */
	@PutMapping("/profile")
	@OperationLogRecord(module = "用户管理", operation = "修改个人信息")
	public ApiResponse<Void> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
		authService.updateProfile(SecurityUtils.getCurrentUserId(), request);
		return ApiResponse.success();
	}

	/**
	 * 修改密码。
	 * @param request 修改密码请求
	 * @return 修改结果
	 */
	@PutMapping("/password")
	@OperationLogRecord(module = "用户管理", operation = "修改密码")
	public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
		authService.changePassword(SecurityUtils.getCurrentUserId(), request);
		return ApiResponse.success();
	}

	/**
	 * 构建令牌 Cookie。
	 * @param value 令牌值
	 * @param maxAge 有效期
	 * @return Cookie
	 */
	private ResponseCookie buildTokenCookie(String value, Duration maxAge) {
		return ResponseCookie.from(JwtAuthenticationFilter.TOKEN_COOKIE, value)
			.httpOnly(true)
			.sameSite("Strict")
			.path("/")
			.maxAge(maxAge)
			.build();
	}

	/**
	 * 从请求 (Cookie 或 Authorization 头) 中解析当前令牌的 jti，用于注销对应会话。
	 * @param request HTTP 请求
	 * @return jti，解析失败返回 null
	 */
	private String resolveJti(HttpServletRequest request) {
		String token = null;
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header != null && header.startsWith("Bearer ")) {
			token = header.substring("Bearer ".length());
		}
		else if (request.getCookies() != null) {
			for (var cookie : request.getCookies()) {
				if (JwtAuthenticationFilter.TOKEN_COOKIE.equals(cookie.getName())) {
					token = cookie.getValue();
					break;
				}
			}
		}
		if (token == null) {
			return null;
		}
		Claims claims = tokenProvider.parse(token);
		return claims == null ? null : claims.getId();
	}

}
