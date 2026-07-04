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
import static org.mockito.Mockito.verifyNoMoreInteractions;
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

	@Test
	@DisplayName("更新用户密码时加密新密码并强制会话下线")
	void updateUserWithPasswordRevokesSessions() {
		User target = user(1L, "alice", Role.USER);
		UserSession session = new UserSession();
		session.setActive(true);
		when(userRepository.findById(1L)).thenReturn(Optional.of(target));
		when(passwordEncoder.encode("NewPass123")).thenReturn("encoded-password");
		when(sessionRepository.findByUserIdAndActiveTrueOrderByCreatedAtAsc(1L))
			.thenReturn(new ArrayList<>(List.of(session)));
		when(userRepository.save(target)).thenReturn(target);

		AdminUserResponse response = service.updateUser(1L,
				new com.tlcsdm.ecovault.dto.UpdateUserRequest("新昵称", "new@ecovault.com", "USER", true, "NewPass123"));

		assertThat(target.getPassword()).isEqualTo("encoded-password");
		assertThat(target.getNickname()).isEqualTo("新昵称");
		assertThat(target.getEmail()).isEqualTo("new@ecovault.com");
		assertThat(session.isActive()).isFalse();
		assertThat(response.username()).isEqualTo("alice");
		verify(sessionRepository).save(session);
	}

	@Test
	@DisplayName("更新用户传入非法角色时抛出业务异常")
	void updateUserRejectsInvalidRole() {
		when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "alice", Role.USER)));

		assertThatThrownBy(() -> service.updateUser(1L,
				new com.tlcsdm.ecovault.dto.UpdateUserRequest(null, null, "unknown", null, null)))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining("角色不合法");
	}

	@Test
	@DisplayName("更新用户遇到空白可选字段时忽略对应修改，但禁用时仍下线会话")
	void updateUserIgnoresBlankFieldsAndDisablesUser() {
		User target = user(1L, "alice", Role.USER);
		target.setPassword("origin-password");
		UserSession session = new UserSession();
		session.setActive(true);
		when(userRepository.findById(1L)).thenReturn(Optional.of(target));
		when(sessionRepository.findByUserIdAndActiveTrueOrderByCreatedAtAsc(1L))
			.thenReturn(new ArrayList<>(List.of(session)));
		when(userRepository.save(target)).thenReturn(target);

		service.updateUser(1L, new com.tlcsdm.ecovault.dto.UpdateUserRequest("   ", null, "   ", false, "   "));

		assertThat(target.getNickname()).isEqualTo("昵称");
		assertThat(target.getRole()).isEqualTo(Role.USER);
		assertThat(target.getPassword()).isEqualTo("origin-password");
		assertThat(target.isEnabled()).isFalse();
		assertThat(session.isActive()).isFalse();
		verify(sessionRepository).save(session);
	}

	@Test
	@DisplayName("更新用户未提供可选字段时保留原值")
	void updateUserKeepsOriginalValuesWhenOptionalFieldsMissing() {
		User target = user(1L, "alice", Role.USER);
		target.setPassword("origin-password");
		when(userRepository.findById(1L)).thenReturn(Optional.of(target));
		when(userRepository.save(target)).thenReturn(target);

		AdminUserResponse response = service.updateUser(1L,
				new com.tlcsdm.ecovault.dto.UpdateUserRequest(null, null, null, null, null));

		assertThat(target.getNickname()).isEqualTo("昵称");
		assertThat(target.getEmail()).isEqualTo("u@ecovault.com");
		assertThat(target.getRole()).isEqualTo(Role.USER);
		assertThat(target.getPassword()).isEqualTo("origin-password");
		assertThat(response.enabled()).isTrue();
		verify(sessionRepository, never()).findByUserIdAndActiveTrueOrderByCreatedAtAsc(any());
	}

	@Test
	@DisplayName("删除用户前先下线活跃会话")
	void deleteUserRevokesSessionsBeforeDelete() {
		User target = user(1L, "alice", Role.USER);
		UserSession session1 = new UserSession();
		session1.setActive(true);
		UserSession session2 = new UserSession();
		session2.setActive(true);
		when(userRepository.findById(1L)).thenReturn(Optional.of(target));
		when(sessionRepository.findByUserIdAndActiveTrueOrderByCreatedAtAsc(1L))
			.thenReturn(new ArrayList<>(List.of(session1, session2)));

		service.deleteUser(1L);

		assertThat(session1.isActive()).isFalse();
		assertThat(session2.isActive()).isFalse();
		verify(sessionRepository).save(session1);
		verify(sessionRepository).save(session2);
		verify(userRepository).delete(target);
		verifyNoMoreInteractions(passwordEncoder);
	}

}
