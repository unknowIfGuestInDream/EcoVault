package com.tlcsdm.ecovault.service.impl;

import com.tlcsdm.ecovault.common.BusinessException;
import com.tlcsdm.ecovault.dto.AdminUserResponse;
import com.tlcsdm.ecovault.dto.UpdateUserRequest;
import com.tlcsdm.ecovault.entity.Role;
import com.tlcsdm.ecovault.entity.User;
import com.tlcsdm.ecovault.entity.UserSession;
import com.tlcsdm.ecovault.repository.UserRepository;
import com.tlcsdm.ecovault.repository.UserSessionRepository;
import com.tlcsdm.ecovault.service.AdminService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理后台服务实现。
 *
 * @author unknowIfGuestInDream
 */
@Service
public class AdminServiceImpl implements AdminService {

	private final UserRepository userRepository;

	private final UserSessionRepository sessionRepository;

	private final PasswordEncoder passwordEncoder;

	public AdminServiceImpl(UserRepository userRepository, UserSessionRepository sessionRepository,
			PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.sessionRepository = sessionRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional(readOnly = true)
	public List<AdminUserResponse> listUsers() {
		return userRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
	}

	@Override
	@Transactional
	public void setUserEnabled(Long userId, boolean enabled) {
		User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException("用户不存在"));
		user.setEnabled(enabled);
		userRepository.save(user);
		// 禁用用户时，强制其所有会话下线
		if (!enabled) {
			List<UserSession> sessions = sessionRepository.findByUserIdAndActiveTrueOrderByCreatedAtAsc(userId);
			for (UserSession session : sessions) {
				session.setActive(false);
				sessionRepository.save(session);
			}
		}
	}

	private AdminUserResponse toResponse(User user) {
		return new AdminUserResponse(user.getId(), user.getUsername(), user.getNickname(), user.getEmail(),
				user.getRole().name(), user.isEnabled(), user.getCreatedAt());
	}

	@Override
	@Transactional
	public AdminUserResponse updateUser(Long userId, UpdateUserRequest request) {
		User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException("用户不存在"));
		if (request.nickname() != null && !request.nickname().isBlank()) {
			user.setNickname(request.nickname().trim());
		}
		if (request.email() != null) {
			user.setEmail(request.email());
		}
		if (request.role() != null && !request.role().isBlank()) {
			user.setRole(parseRole(request.role()));
		}
		if (request.password() != null && !request.password().isBlank()) {
			user.setPassword(passwordEncoder.encode(request.password()));
			revokeSessions(userId);
		}
		if (request.enabled() != null) {
			user.setEnabled(request.enabled());
			if (!request.enabled()) {
				revokeSessions(userId);
			}
		}
		return toResponse(userRepository.save(user));
	}

	@Override
	@Transactional
	public void deleteUser(Long userId) {
		User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException("用户不存在"));
		// 删除用户前先下线其所有会话
		revokeSessions(userId);
		userRepository.delete(user);
	}

	private void revokeSessions(Long userId) {
		List<UserSession> sessions = sessionRepository.findByUserIdAndActiveTrueOrderByCreatedAtAsc(userId);
		for (UserSession session : sessions) {
			session.setActive(false);
			sessionRepository.save(session);
		}
	}

	private Role parseRole(String role) {
		try {
			return Role.valueOf(role.trim().toUpperCase());
		}
		catch (IllegalArgumentException ex) {
			throw new BusinessException("角色不合法");
		}
	}

}
