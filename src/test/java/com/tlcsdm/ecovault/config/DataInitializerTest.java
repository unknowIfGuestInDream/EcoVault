package com.tlcsdm.ecovault.config;

import com.tlcsdm.ecovault.entity.Role;
import com.tlcsdm.ecovault.entity.User;
import com.tlcsdm.ecovault.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 数据初始化器单元测试。
 *
 * @author unknowIfGuestInDream
 */
@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	@Test
	@DisplayName("管理员不存在时创建默认管理员且密码经 BCrypt 加密")
	void createsAdminWhenAbsent() throws Exception {
		UserRepository userRepository = mock(UserRepository.class);
		when(userRepository.existsByUsername("admin")).thenReturn(false);
		DataInitializer initializer = new DataInitializer(userRepository, passwordEncoder, "admin", "Admin@123");

		initializer.run();

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(captor.capture());
		User admin = captor.getValue();
		assertThat(admin.getUsername()).isEqualTo("admin");
		assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
		assertThat(admin.isEnabled()).isTrue();
		assertThat(passwordEncoder.matches("Admin@123", admin.getPassword())).isTrue();
	}

	@Test
	@DisplayName("管理员已存在时不重复创建")
	void skipsWhenAdminExists() throws Exception {
		UserRepository userRepository = mock(UserRepository.class);
		when(userRepository.existsByUsername("admin")).thenReturn(true);
		DataInitializer initializer = new DataInitializer(userRepository, passwordEncoder, "admin", "Admin@123");

		initializer.run();

		verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
	}

}
