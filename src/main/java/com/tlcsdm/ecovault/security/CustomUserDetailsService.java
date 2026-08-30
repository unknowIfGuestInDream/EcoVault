package com.tlcsdm.ecovault.security;

import com.tlcsdm.ecovault.entity.User;
import com.tlcsdm.ecovault.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 自定义用户详情服务，从数据库加载用户。
 *
 * @author unknowIfGuestInDream
 * @since 1.0.0
 * @see UserDetailsService
 */
@Service

public class CustomUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	/**
	 * 构造CustomUserDetailsService实例并注入所需依赖。
	 * @param userRepository userRepository参数。
	 */
	public CustomUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	/**
	 * 根据用户名加载认证所需的用户详情。
	 * @param username 用户名。
	 * @return 适配后的用户详情对象。
	 * @throws UsernameNotFoundException 当指定用户名不存在时抛出。
	 */
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User user = userRepository.findByUsername(username)
			.orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));
		return new SecurityUser(user);
	}

}
