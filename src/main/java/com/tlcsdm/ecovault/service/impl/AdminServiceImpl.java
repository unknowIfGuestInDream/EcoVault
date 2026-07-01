package com.tlcsdm.ecovault.service.impl;

import com.tlcsdm.ecovault.common.BusinessException;
import com.tlcsdm.ecovault.dto.AdminUserResponse;
import com.tlcsdm.ecovault.entity.User;
import com.tlcsdm.ecovault.entity.UserSession;
import com.tlcsdm.ecovault.repository.UserRepository;
import com.tlcsdm.ecovault.repository.UserSessionRepository;
import com.tlcsdm.ecovault.service.AdminService;
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

	public AdminServiceImpl(UserRepository userRepository, UserSessionRepository sessionRepository) {
		this.userRepository = userRepository;
		this.sessionRepository = sessionRepository;
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

}
