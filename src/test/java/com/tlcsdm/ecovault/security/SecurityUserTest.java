package com.tlcsdm.ecovault.security;

import com.tlcsdm.ecovault.entity.Role;
import com.tlcsdm.ecovault.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SecurityUser 主体单元测试。
 *
 * @author unknowIfGuestInDream
 */
class SecurityUserTest {

	private SecurityUser build(Role role, boolean enabled) {
		User user = new User();
		user.setId(10L);
		user.setUsername("alice");
		user.setPassword("hashed");
		user.setRole(role);
		user.setEnabled(enabled);
		return new SecurityUser(user);
	}

	@Test
	@DisplayName("包装用户实体并暴露 ID/用户名/密码")
	void basicAccessors() {
		SecurityUser securityUser = build(Role.USER, true);
		assertThat(securityUser.getId()).isEqualTo(10L);
		assertThat(securityUser.getUsername()).isEqualTo("alice");
		assertThat(securityUser.getPassword()).isEqualTo("hashed");
		assertThat(securityUser.getUser().getUsername()).isEqualTo("alice");
	}

	@Test
	@DisplayName("角色转换为 ROLE_ 前缀权限")
	void authorities() {
		SecurityUser admin = build(Role.ADMIN, true);
		assertThat(admin.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_ADMIN");

		SecurityUser user = build(Role.USER, true);
		assertThat(user.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_USER");
	}

	@Test
	@DisplayName("账户状态标志：非过期/非锁定/凭据有效，启用状态跟随实体")
	void statusFlags() {
		SecurityUser enabled = build(Role.USER, true);
		assertThat(enabled.isAccountNonExpired()).isTrue();
		assertThat(enabled.isAccountNonLocked()).isTrue();
		assertThat(enabled.isCredentialsNonExpired()).isTrue();
		assertThat(enabled.isEnabled()).isTrue();

		SecurityUser disabled = build(Role.USER, false);
		assertThat(disabled.isEnabled()).isFalse();
	}

}
