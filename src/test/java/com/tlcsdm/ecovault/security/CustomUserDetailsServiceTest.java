package com.tlcsdm.ecovault.security;

import com.tlcsdm.ecovault.entity.Role;
import com.tlcsdm.ecovault.entity.User;
import com.tlcsdm.ecovault.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * 自定义用户详情服务单元测试。
 *
 * @author unknowIfGuestInDream
 */
@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private CustomUserDetailsService service;

	@Test
	@DisplayName("按用户名加载存在的用户返回 SecurityUser")
	void loadExisting() {
		User user = new User();
		user.setId(1L);
		user.setUsername("alice");
		user.setRole(Role.USER);
		user.setEnabled(true);
		when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

		UserDetails details = service.loadUserByUsername("alice");

		assertThat(details).isInstanceOf(SecurityUser.class);
		assertThat(details.getUsername()).isEqualTo("alice");
	}

	@Test
	@DisplayName("用户不存在时抛出 UsernameNotFoundException")
	void loadMissing() {
		when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
		assertThatThrownBy(() -> service.loadUserByUsername("ghost")).isInstanceOf(UsernameNotFoundException.class)
			.hasMessageContaining("ghost");
	}

}
