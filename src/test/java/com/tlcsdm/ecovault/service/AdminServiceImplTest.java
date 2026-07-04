package com.tlcsdm.ecovault.service;

import com.tlcsdm.ecovault.common.BusinessException;
import com.tlcsdm.ecovault.dto.AdminUserResponse;
import com.tlcsdm.ecovault.entity.Role;
import com.tlcsdm.ecovault.entity.User;
import com.tlcsdm.ecovault.entity.UserSession;
import com.tlcsdm.ecovault.repository.UserRepository;
import com.tlcsdm.ecovault.repository.UserSessionRepository;
import com.tlcsdm.ecovault.service.impl.AdminServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 管理后台服务实现单元测试。
 *
 * @author unknowIfGuestInDream
 */
@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private UserSessionRepository sessionRepository;

	@Mock
	private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

	@InjectMocks
	private AdminServiceImpl service;

	private User user(Long id, String username, Role role) {
		User user = new User();
		user.setId(id);
		user.setUsername(username);
		user.setNickname("昵称");
		user.setEmail("u@ecovault.com");
		user.setRole(role);
		user.setEnabled(true);
		user.setCreatedAt(LocalDateTime.now());
		return user;
	}

	@Test
	@DisplayName("列出全部用户并映射为响应")
	void listUsers() {
		when(userRepository.findAll()).thenReturn(List.of(user(1L, "alice", Role.USER), user(2L, "admin", Role.ADMIN)));

		List<AdminUserResponse> responses = service.listUsers();

		assertThat(responses).hasSize(2);
		assertThat(responses.get(0).username()).isEqualTo("alice");
		assertThat(responses.get(0).role()).isEqualTo("USER");
		assertThat(responses.get(1).role()).isEqualTo("ADMIN");
		assertThat(responses.get(0).enabled()).isTrue();
	}

	@Test
	@DisplayName("启用用户仅更新状态，不触及会话")
	void enableUser() {
		User target = user(1L, "alice", Role.USER);
		target.setEnabled(false);
		when(userRepository.findById(1L)).thenReturn(Optional.of(target));

		service.setUserEnabled(1L, true);

		assertThat(target.isEnabled()).isTrue();
		verify(userRepository).save(target);
		verify(sessionRepository, never()).findByUserIdAndActiveTrueOrderByCreatedAtAsc(any());
	}

	@Test
	@DisplayName("禁用用户时强制其所有活跃会话下线")
	void disableUserRevokesSessions() {
		User target = user(1L, "alice", Role.USER);
		when(userRepository.findById(1L)).thenReturn(Optional.of(target));
		UserSession s1 = new UserSession();
		s1.setActive(true);
		UserSession s2 = new UserSession();
		s2.setActive(true);
		when(sessionRepository.findByUserIdAndActiveTrueOrderByCreatedAtAsc(1L))
			.thenReturn(new ArrayList<>(List.of(s1, s2)));

		service.setUserEnabled(1L, false);

		assertThat(target.isEnabled()).isFalse();
		assertThat(s1.isActive()).isFalse();
		assertThat(s2.isActive()).isFalse();
		verify(sessionRepository).save(s1);
		verify(sessionRepository).save(s2);
	}

	@Test
	@DisplayName("修改不存在的用户状态抛出业务异常")
	void missingUserThrows() {
		when(userRepository.findById(99L)).thenReturn(Optional.empty());
		assertThatThrownBy(() -> service.setUserEnabled(99L, true)).isInstanceOf(BusinessException.class);
	}

}
