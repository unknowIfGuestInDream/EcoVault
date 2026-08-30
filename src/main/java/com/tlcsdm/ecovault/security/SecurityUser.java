package com.tlcsdm.ecovault.security;

import com.tlcsdm.ecovault.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security 用户主体，包装 {@link User} 实体，便于在认证上下文中获取用户 ID 与角色。
 *
 * @author unknowIfGuestInDream
 * @since 1.0.0
 * @see UserDetails
 */
public class SecurityUser implements UserDetails {

	private final User user;

	/**
	 * 构造SecurityUser实例并注入所需依赖。
	 * @param user 用户实体。
	 */
	public SecurityUser(User user) {
		this.user = user;
	}

	/**
	 * 获取底层用户实体。
	 * @return 用户实体
	 */
	public User getUser() {
		return user;
	}

	/**
	 * 获取用户 ID。
	 * @return 用户 ID
	 */
	public Long getId() {
		return user.getId();
	}

	/**
	 * 获取相关属性值。
	 * @return 方法执行结果。
	 */
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		// Spring Security 约定角色以 ROLE_ 前缀表示
		return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
	}

	/**
	 * 获取相关属性值。
	 * @return 密码字段内容。
	 */
	@Override
	public String getPassword() {
		return user.getPassword();
	}

	/**
	 * 获取相关属性值。
	 * @return 用户名。
	 */
	@Override
	public String getUsername() {
		return user.getUsername();
	}

	/**
	 * 判断相关状态。
	 * @return 方法执行结果。
	 */
	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	/**
	 * 判断相关状态。
	 * @return 方法执行结果。
	 */
	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	/**
	 * 判断相关状态。
	 * @return 方法执行结果。
	 */
	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	/**
	 * 判断相关状态。
	 * @return 是否启用。
	 */
	@Override
	public boolean isEnabled() {
		return user.isEnabled();
	}

}
