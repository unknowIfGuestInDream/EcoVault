package com.tlcsdm.ecovault.security;

import com.tlcsdm.ecovault.common.BusinessException;
import com.tlcsdm.ecovault.entity.Role;
import com.tlcsdm.ecovault.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 安全上下文工具单元测试。
 *
 * @author unknowIfGuestInDream
 */
class SecurityUtilsTest {

	@AfterEach
	void clear() {
		SecurityContextHolder.clearContext();
	}

	private SecurityUser securityUser(Long id, String username, Role role) {
		User user = new User();
		user.setId(id);
		user.setUsername(username);
		user.setRole(role);
		user.setEnabled(true);
		return new SecurityUser(user);
	}

	private void authenticate(SecurityUser principal) {
		UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null,
				principal.getAuthorities());
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	@Test
	@DisplayName("已登录时可获取当前用户/ID/用户名")
	void currentUser() {
		authenticate(securityUser(1L, "alice", Role.USER));
		assertThat(SecurityUtils.getCurrentUser().getUsername()).isEqualTo("alice");
		assertThat(SecurityUtils.getCurrentUserId()).isEqualTo(1L);
		assertThat(SecurityUtils.getCurrentUsername()).isEqualTo("alice");
	}

	@Test
	@DisplayName("未登录时获取当前用户抛出 401 业务异常")
	void notLoggedIn() {
		assertThatThrownBy(SecurityUtils::getCurrentUser).isInstanceOf(BusinessException.class)
			.satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(401));
	}

	@Test
	@DisplayName("认证主体非 SecurityUser 时抛出未登录异常")
	void wrongPrincipalType() {
		UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("plain", null,
				List.of(new SimpleGrantedAuthority("ROLE_USER")));
		SecurityContextHolder.getContext().setAuthentication(auth);
		assertThatThrownBy(SecurityUtils::getCurrentUser).isInstanceOf(BusinessException.class);
	}

	@Test
	@DisplayName("管理员角色判定为 true")
	void isAdminTrue() {
		authenticate(securityUser(2L, "admin", Role.ADMIN));
		assertThat(SecurityUtils.isAdmin()).isTrue();
	}

	@Test
	@DisplayName("普通用户判定为非管理员")
	void isAdminFalse() {
		authenticate(securityUser(3L, "user", Role.USER));
		assertThat(SecurityUtils.isAdmin()).isFalse();
	}

	@Test
	@DisplayName("未认证时判定为非管理员")
	void isAdminNoAuth() {
		assertThat(SecurityUtils.isAdmin()).isFalse();
	}

}
