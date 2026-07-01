package com.tlcsdm.ecovault.service;

import com.tlcsdm.ecovault.common.BusinessException;
import com.tlcsdm.ecovault.dto.ChangePasswordRequest;
import com.tlcsdm.ecovault.dto.LoginRequest;
import com.tlcsdm.ecovault.dto.LoginResponse;
import com.tlcsdm.ecovault.dto.RegisterRequest;
import com.tlcsdm.ecovault.entity.Role;
import com.tlcsdm.ecovault.entity.User;
import com.tlcsdm.ecovault.entity.UserSession;
import com.tlcsdm.ecovault.repository.UserRepository;
import com.tlcsdm.ecovault.repository.UserSessionRepository;
import com.tlcsdm.ecovault.security.JwtTokenProvider;
import com.tlcsdm.ecovault.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * 认证服务单元测试。
 *
 * @author unknowIfGuestInDream
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private UserSessionRepository sessionRepository;

	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	private final JwtTokenProvider tokenProvider = new JwtTokenProvider("test-secret-key-for-unit-tests", 7200000L);

	private AuthServiceImpl service;

	@BeforeEach
	void setUp() {
		// 默认单设备
		service = new AuthServiceImpl(userRepository, sessionRepository, passwordEncoder, tokenProvider, 1);
	}

	private User existingUser() {
		User user = new User();
		user.setId(1L);
		user.setUsername("alice");
		user.setPassword(passwordEncoder.encode("Passw0rd!"));
		user.setNickname("Alice");
		user.setRole(Role.USER);
		user.setEnabled(true);
		return user;
	}

	@Test
	@DisplayName("注册重复用户名抛出异常")
	void registerDuplicate() {
		when(userRepository.existsByUsername("alice")).thenReturn(true);
		RegisterRequest request = new RegisterRequest("alice", "Passw0rd!", "Alice", "a@b.com");

		assertThatThrownBy(() -> service.register(request)).isInstanceOf(BusinessException.class);
	}

	@Test
	@DisplayName("注册使用 BCrypt 加密密码存储")
	void registerEncodesPassword() {
		when(userRepository.existsByUsername("bob")).thenReturn(false);
		when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
		RegisterRequest request = new RegisterRequest("bob", "Secret123", null, null);

		User saved = service.register(request);

		assertThat(saved.getPassword()).isNotEqualTo("Secret123");
		assertThat(passwordEncoder.matches("Secret123", saved.getPassword())).isTrue();
		assertThat(saved.getNickname()).isEqualTo("bob"); // 昵称为空时回退用户名
		assertThat(saved.getRole()).isEqualTo(Role.USER);
	}

	@Test
	@DisplayName("密码错误登录失败")
	void loginWrongPassword() {
		when(userRepository.findByUsername("alice")).thenReturn(Optional.of(existingUser()));
		LoginRequest request = new LoginRequest("alice", "wrong");

		assertThatThrownBy(() -> service.login(request, "device", "127.0.0.1")).isInstanceOf(BusinessException.class);
	}

	@Test
	@DisplayName("被禁用账户登录失败")
	void loginDisabled() {
		User user = existingUser();
		user.setEnabled(false);
		when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
		LoginRequest request = new LoginRequest("alice", "Passw0rd!");

		assertThatThrownBy(() -> service.login(request, "device", "127.0.0.1")).isInstanceOf(BusinessException.class);
	}

	@Test
	@DisplayName("登录成功返回令牌并创建会话")
	void loginSuccess() {
		when(userRepository.findByUsername("alice")).thenReturn(Optional.of(existingUser()));
		when(sessionRepository.findByUserIdAndActiveTrueOrderByCreatedAtAsc(1L)).thenReturn(new ArrayList<>());
		when(sessionRepository.save(any(UserSession.class))).thenAnswer(inv -> inv.getArgument(0));
		LoginRequest request = new LoginRequest("alice", "Passw0rd!");

		LoginResponse response = service.login(request, "chrome", "127.0.0.1");

		assertThat(response.token()).isNotBlank();
		assertThat(response.username()).isEqualTo("alice");
		assertThat(tokenProvider.parse(response.token())).isNotNull();
	}

	@Test
	@DisplayName("单设备限制：登录时下线已有会话")
	void singleDeviceEnforcement() {
		UserSession old = new UserSession();
		old.setUserId(1L);
		old.setJti("old-jti");
		old.setActive(true);

		when(userRepository.findByUsername("alice")).thenReturn(Optional.of(existingUser()));
		List<UserSession> active = new ArrayList<>(List.of(old));
		when(sessionRepository.findByUserIdAndActiveTrueOrderByCreatedAtAsc(1L)).thenReturn(active);
		when(sessionRepository.save(any(UserSession.class))).thenAnswer(inv -> inv.getArgument(0));

		service.login(new LoginRequest("alice", "Passw0rd!"), "device", "127.0.0.1");

		// 旧会话应被置为失效
		assertThat(old.isActive()).isFalse();
	}

	@Test
	@DisplayName("修改密码校验原密码并使所有会话失效")
	void changePasswordInvalidatesSessions() {
		User user = existingUser();
		UserSession session = new UserSession();
		session.setUserId(1L);
		session.setActive(true);

		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
		List<UserSession> sessions = new ArrayList<>(List.of(session));
		lenient().when(sessionRepository.findByUserIdAndActiveTrueOrderByCreatedAtAsc(1L)).thenReturn(sessions);

		service.changePassword(1L, new ChangePasswordRequest("Passw0rd!", "NewPass123"));

		assertThat(passwordEncoder.matches("NewPass123", user.getPassword())).isTrue();
		assertThat(session.isActive()).isFalse();
	}

	@Test
	@DisplayName("注册提供非空昵称时保留该昵称")
	void registerKeepsProvidedNickname() {
		when(userRepository.existsByUsername("carol")).thenReturn(false);
		when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

		User saved = service.register(new RegisterRequest("carol", "Secret123", "卡罗尔", "c@d.com"));

		assertThat(saved.getNickname()).isEqualTo("卡罗尔");
		assertThat(saved.getEmail()).isEqualTo("c@d.com");
	}

	@Test
	@DisplayName("用户名不存在登录失败")
	void loginUserNotFound() {
		when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.login(new LoginRequest("ghost", "x"), "device", "127.0.0.1"))
			.isInstanceOf(BusinessException.class);
	}

	@Test
	@DisplayName("超长设备信息在会话中被截断至上限长度")
	void loginTruncatesLongDeviceInfo() {
		when(userRepository.findByUsername("alice")).thenReturn(Optional.of(existingUser()));
		when(sessionRepository.findByUserIdAndActiveTrueOrderByCreatedAtAsc(1L)).thenReturn(new ArrayList<>());
		List<UserSession> captured = new ArrayList<>();
		when(sessionRepository.save(any(UserSession.class))).thenAnswer(inv -> {
			captured.add(inv.getArgument(0));
			return inv.getArgument(0);
		});
		String longDevice = "d".repeat(600);

		service.login(new LoginRequest("alice", "Passw0rd!"), longDevice, "127.0.0.1");

		assertThat(captured.get(0).getDeviceInfo()).hasSize(512);
	}

	@Test
	@DisplayName("登出 jti 为空时直接返回，不访问仓储")
	void logoutNullJti() {
		service.logout(null);

		org.mockito.Mockito.verifyNoInteractions(sessionRepository);
	}

	@Test
	@DisplayName("登出存在的会话将其置为失效")
	void logoutExistingSession() {
		UserSession session = new UserSession();
		session.setJti("jti-1");
		session.setActive(true);
		when(sessionRepository.findByJti("jti-1")).thenReturn(Optional.of(session));
		when(sessionRepository.save(any(UserSession.class))).thenAnswer(inv -> inv.getArgument(0));

		service.logout("jti-1");

		assertThat(session.isActive()).isFalse();
	}

	@Test
	@DisplayName("登出不存在的会话时安全忽略")
	void logoutMissingSession() {
		when(sessionRepository.findByJti("none")).thenReturn(Optional.empty());

		service.logout("none");

		org.mockito.Mockito.verify(sessionRepository).findByJti("none");
	}

	@Test
	@DisplayName("更新资料非空昵称时更新昵称与邮箱")
	void updateProfileWithNickname() {
		User user = existingUser();
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

		User updated = service.updateProfile(1L,
				new com.tlcsdm.ecovault.dto.UpdateProfileRequest("新昵称", "new@mail.com"));

		assertThat(updated.getNickname()).isEqualTo("新昵称");
		assertThat(updated.getEmail()).isEqualTo("new@mail.com");
	}

	@Test
	@DisplayName("更新资料昵称为空白时保留原昵称")
	void updateProfileBlankNicknameKept() {
		User user = existingUser();
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

		User updated = service.updateProfile(1L, new com.tlcsdm.ecovault.dto.UpdateProfileRequest("  ", "e@f.com"));

		assertThat(updated.getNickname()).isEqualTo("Alice");
		assertThat(updated.getEmail()).isEqualTo("e@f.com");
	}

	@Test
	@DisplayName("更新不存在用户资料抛出异常")
	void updateProfileUserNotFound() {
		when(userRepository.findById(9L)).thenReturn(Optional.empty());

		assertThatThrownBy(
				() -> service.updateProfile(9L, new com.tlcsdm.ecovault.dto.UpdateProfileRequest("n", "e@f.com")))
			.isInstanceOf(BusinessException.class);
	}

	@Test
	@DisplayName("修改不存在用户密码抛出异常")
	void changePasswordUserNotFound() {
		when(userRepository.findById(9L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.changePassword(9L, new ChangePasswordRequest("a", "Newpass123")))
			.isInstanceOf(BusinessException.class);
	}

	@Test
	@DisplayName("最大设备数小于 1 时回退为 1")
	void maxDevicesFloorsToOne() {
		AuthServiceImpl zeroDevice = new AuthServiceImpl(userRepository, sessionRepository, passwordEncoder,
				tokenProvider, 0);

		when(userRepository.findByUsername("alice")).thenReturn(Optional.of(existingUser()));
		when(sessionRepository.findByUserIdAndActiveTrueOrderByCreatedAtAsc(1L)).thenReturn(new ArrayList<>());
		when(sessionRepository.save(any(UserSession.class))).thenAnswer(inv -> inv.getArgument(0));

		LoginResponse response = zeroDevice.login(new LoginRequest("alice", "Passw0rd!"), "device", "127.0.0.1");

		assertThat(response.token()).isNotBlank();
	}

}
