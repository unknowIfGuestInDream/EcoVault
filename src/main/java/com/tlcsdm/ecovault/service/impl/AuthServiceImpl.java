package com.tlcsdm.ecovault.service.impl;

import com.tlcsdm.ecovault.common.BusinessException;
import com.tlcsdm.ecovault.dto.ChangePasswordRequest;
import com.tlcsdm.ecovault.dto.LoginRequest;
import com.tlcsdm.ecovault.dto.LoginResponse;
import com.tlcsdm.ecovault.dto.RegisterRequest;
import com.tlcsdm.ecovault.dto.UpdateProfileRequest;
import com.tlcsdm.ecovault.entity.Role;
import com.tlcsdm.ecovault.entity.User;
import com.tlcsdm.ecovault.entity.UserSession;
import com.tlcsdm.ecovault.repository.UserRepository;
import com.tlcsdm.ecovault.repository.UserSessionRepository;
import com.tlcsdm.ecovault.security.JwtTokenProvider;
import com.tlcsdm.ecovault.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 认证与用户信息服务实现。
 *
 * @author unknowIfGuestInDream
 * @since 1.0.0
 * @see AuthService
 */
@Service
public class AuthServiceImpl implements AuthService {

	private final UserRepository userRepository;

	private final UserSessionRepository sessionRepository;

	private final PasswordEncoder passwordEncoder;

	private final JwtTokenProvider tokenProvider;

	/** 单用户允许的最大在线设备数 */
	private final int maxDevices;

	public AuthServiceImpl(UserRepository userRepository, UserSessionRepository sessionRepository,
			PasswordEncoder passwordEncoder, JwtTokenProvider tokenProvider,
			@Value("${ecovault.security.max-devices:1}") int maxDevices) {
		this.userRepository = userRepository;
		this.sessionRepository = sessionRepository;
		this.passwordEncoder = passwordEncoder;
		this.tokenProvider = tokenProvider;
		this.maxDevices = Math.max(1, maxDevices);
	}

	/**
	 * 创建普通用户账户。
	 * @param request 请求参数对象。
	 * @return 创建后的用户信息。
	 */
	@Override
	@Transactional
	public User register(RegisterRequest request) {
		if (userRepository.existsByUsername(request.username())) {
			throw new BusinessException("用户名已存在");
		}
		User user = new User();
		user.setUsername(request.username());
		// 使用 BCrypt 加密存储密码
		user.setPassword(passwordEncoder.encode(request.password()));
		user.setNickname(
				request.nickname() == null || request.nickname().isBlank() ? request.username() : request.nickname());
		user.setEmail(request.email());
		user.setRole(parseRole(request.role()));
		user.setEnabled(true);
		return userRepository.save(user);
	}

	/**
	 * 解析请求中的角色，非法或为空时默认为 {@link Role#USER}。
	 * @param role 角色名称
	 * @return 角色枚举
	 */
	private Role parseRole(String role) {
		if (role == null || role.isBlank()) {
			return Role.USER;
		}
		try {
			return Role.valueOf(role.trim().toUpperCase());
		}
		catch (IllegalArgumentException ex) {
			throw new BusinessException("角色不合法");
		}
	}

	/**
	 * 处理用户登录请求。
	 * @param request 请求参数对象。
	 * @param deviceInfo 设备信息。
	 * @param ip 客户端 IP 地址。
	 * @return 登录结果。
	 */
	@Override
	@Transactional
	public LoginResponse login(LoginRequest request, String deviceInfo, String ip) {
		User user = userRepository.findByUsername(request.username())
			.orElseThrow(() -> new BusinessException("用户名或密码错误"));
		if (!passwordEncoder.matches(request.password(), user.getPassword())) {
			throw new BusinessException("用户名或密码错误");
		}
		if (!user.isEnabled()) {
			throw new BusinessException("账户已被禁用，请联系管理员");
		}

		// 单设备登录限制：超出最大设备数时下线最早的会话
		enforceDeviceLimit(user.getId());

		String jti = tokenProvider.newJti();
		String token = tokenProvider.generateToken(user.getUsername(), user.getId(), jti);

		UserSession session = new UserSession();
		session.setUserId(user.getId());
		session.setJti(jti);
		session.setDeviceInfo(truncate(deviceInfo, 512));
		session.setIp(ip);
		session.setActive(true);
		sessionRepository.save(session);

		return new LoginResponse(token, user.getUsername(), user.getNickname(), user.getRole().name());
	}

	/**
	 * 强制执行设备数量限制：保留最新的 (maxDevices-1) 个会话，其余置为失效。
	 * @param userId 用户 ID
	 */
	private void enforceDeviceLimit(Long userId) {
		List<UserSession> activeSessions = sessionRepository.findByUserIdAndActiveTrueOrderByCreatedAtAsc(userId);
		int allowedRemaining = maxDevices - 1;
		int toRevoke = activeSessions.size() - allowedRemaining;
		for (int i = 0; i < toRevoke; i++) {
			UserSession session = activeSessions.get(i);
			session.setActive(false);
			sessionRepository.save(session);
		}
	}

	/**
	 * 处理用户注销请求。
	 * @param jti 会话唯一标识。
	 */
	@Override
	@Transactional
	public void logout(String jti) {
		if (jti == null) {
			return;
		}
		sessionRepository.findByJti(jti).ifPresent(session -> {
			session.setActive(false);
			sessionRepository.save(session);
		});
	}

	/**
	 * 更新已有业务数据。
	 * @param userId 用户编号。
	 * @param request 请求参数对象。
	 * @return 更新后的业务结果。
	 */
	@Override
	@Transactional
	public User updateProfile(Long userId, UpdateProfileRequest request) {
		User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException("用户不存在"));
		if (request.nickname() != null && !request.nickname().isBlank()) {
			user.setNickname(request.nickname());
		}
		user.setEmail(request.email());
		return userRepository.save(user);
	}

	/**
	 * 修改当前用户密码。
	 * @param userId 用户编号。
	 * @param request 请求参数对象。
	 */
	@Override
	@Transactional
	public void changePassword(Long userId, ChangePasswordRequest request) {
		User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException("用户不存在"));
		if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
			throw new BusinessException("原密码不正确");
		}
		user.setPassword(passwordEncoder.encode(request.newPassword()));
		userRepository.save(user);
		// 修改密码后使该用户所有会话失效，需重新登录
		sessionRepository.findByUserIdAndActiveTrueOrderByCreatedAtAsc(userId).forEach(session -> {
			session.setActive(false);
			sessionRepository.save(session);
		});
	}

	/**
	 * 校验用户输入密码是否正确。
	 * @param userId 用户编号。
	 * @param rawPassword 待校验的明文密码。
	 */
	@Override
	@Transactional(readOnly = true)
	public void verifyPassword(Long userId, String rawPassword) {
		User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException("用户不存在"));
		// 仅校验密码，不签发新令牌、不刷新会话，隐私模式解锁专用
		if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
			throw new BusinessException("密码错误");
		}
	}

	private String truncate(String value, int maxLength) {
		if (value == null) {
			return null;
		}
		return value.length() > maxLength ? value.substring(0, maxLength) : value;
	}

}
