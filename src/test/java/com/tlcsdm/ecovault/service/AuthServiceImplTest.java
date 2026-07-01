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

    private final JwtTokenProvider tokenProvider =
            new JwtTokenProvider("test-secret-key-for-unit-tests", 86400000L);

    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        // 默认单设备
        service = new AuthServiceImpl(userRepository, sessionRepository,
                passwordEncoder, tokenProvider, 1);
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

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(BusinessException.class);
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

        assertThatThrownBy(() -> service.login(request, "device", "127.0.0.1"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("被禁用账户登录失败")
    void loginDisabled() {
        User user = existingUser();
        user.setEnabled(false);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        LoginRequest request = new LoginRequest("alice", "Passw0rd!");

        assertThatThrownBy(() -> service.login(request, "device", "127.0.0.1"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("登录成功返回令牌并创建会话")
    void loginSuccess() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(existingUser()));
        when(sessionRepository.findByUserIdAndActiveTrueOrderByCreatedAtAsc(1L))
                .thenReturn(new ArrayList<>());
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
        lenient().when(sessionRepository.findByUserIdAndActiveTrueOrderByCreatedAtAsc(1L))
                .thenReturn(sessions);

        service.changePassword(1L, new ChangePasswordRequest("Passw0rd!", "NewPass123"));

        assertThat(passwordEncoder.matches("NewPass123", user.getPassword())).isTrue();
        assertThat(session.isActive()).isFalse();
    }

    @Test
    @DisplayName("修改密码原密码错误抛出异常")
    void changePasswordWrongOld() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser()));

        assertThatThrownBy(() ->
                service.changePassword(1L, new ChangePasswordRequest("wrong", "NewPass123")))
                .isInstanceOf(BusinessException.class);
    }
}
