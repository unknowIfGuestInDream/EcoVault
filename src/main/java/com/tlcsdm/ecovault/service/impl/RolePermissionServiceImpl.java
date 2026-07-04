package com.tlcsdm.ecovault.service.impl;

import com.tlcsdm.ecovault.common.BusinessException;
import com.tlcsdm.ecovault.dto.RoleMatrixResponse;
import com.tlcsdm.ecovault.entity.MenuPage;
import com.tlcsdm.ecovault.entity.Role;
import com.tlcsdm.ecovault.entity.RolePermission;
import com.tlcsdm.ecovault.entity.User;
import com.tlcsdm.ecovault.repository.RolePermissionRepository;
import com.tlcsdm.ecovault.service.RolePermissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 角色-页面权限服务实现。
 *
 * @author unknowIfGuestInDream
 */
@Service
public class RolePermissionServiceImpl implements RolePermissionService {

	private final RolePermissionRepository repository;

	public RolePermissionServiceImpl(RolePermissionRepository repository) {
		this.repository = repository;
	}

	@Override
	@Transactional
	public void initDefaults() {
		// 默认所有角色可访问全部可配置页面，管理员可在角色管理中按需收紧
		List<String> allConfigurable = MenuPage.configurablePages().stream().map(MenuPage::getKey).toList();
		for (Role role : Role.values()) {
			if (!repository.existsByRole(role)) {
				for (String key : allConfigurable) {
					repository.save(new RolePermission(role, key));
				}
			}
		}
	}

	@Override
	@Transactional(readOnly = true)
	public RoleMatrixResponse getMatrix() {
		List<RoleMatrixResponse.PageInfo> pages = MenuPage.configurablePages()
			.stream()
			.map(p -> new RoleMatrixResponse.PageInfo(p.getKey(), p.getLabel(), p.getGroup().name()))
			.collect(Collectors.toList());

		List<RoleMatrixResponse.RolePermissionView> roles = new java.util.ArrayList<>();
		for (Role role : Role.values()) {
			List<String> allowed = allowedConfigurableKeys(role).stream().toList();
			roles.add(new RoleMatrixResponse.RolePermissionView(role.name(), allowed));
		}
		return new RoleMatrixResponse(pages, roles);
	}

	@Override
	@Transactional
	public void updatePermissions(Role role, List<String> pageKeys) {
		Set<String> configurableKeys = MenuPage.configurablePages()
			.stream()
			.map(MenuPage::getKey)
			.collect(Collectors.toSet());
		Set<String> normalized = new LinkedHashSet<>();
		if (pageKeys != null) {
			for (String key : pageKeys) {
				if (key == null || key.isBlank()) {
					continue;
				}
				String trimmed = key.trim();
				if (!configurableKeys.contains(trimmed)) {
					throw new BusinessException("非法的页面: " + trimmed);
				}
				normalized.add(trimmed);
			}
		}
		repository.deleteByRole(role);
		repository.flush();
		for (String key : normalized) {
			repository.save(new RolePermission(role, key));
		}
	}

	@Override
	@Transactional(readOnly = true)
	public Set<String> accessiblePageKeys(User user) {
		Set<String> keys = new LinkedHashSet<>();
		// 控制台与个人中心对所有登录用户开放
		for (MenuPage page : MenuPage.values()) {
			if (!page.isAdminOnly() && !page.isConfigurable()) {
				keys.add(page.getKey());
			}
		}
		if (user != null && user.getRole() == Role.ADMIN) {
			// 管理员可访问全部页面
			for (MenuPage page : MenuPage.values()) {
				keys.add(page.getKey());
			}
			return keys;
		}
		if (user != null) {
			keys.addAll(allowedConfigurableKeys(user.getRole()));
		}
		return keys;
	}

	@Override
	@Transactional(readOnly = true)
	public boolean canAccessPath(User user, String path) {
		Optional<MenuPage> pageOpt = java.util.Arrays.stream(MenuPage.values())
			.filter(p -> p.getPath().equals(path))
			.findFirst();
		if (pageOpt.isEmpty()) {
			return true;
		}
		MenuPage page = pageOpt.get();
		boolean admin = user != null && user.getRole() == Role.ADMIN;
		if (page.isAdminOnly()) {
			return admin;
		}
		if (admin) {
			return true;
		}
		if (page.isConfigurable()) {
			return user != null && allowedConfigurableKeys(user.getRole()).contains(page.getKey());
		}
		return true;
	}

	private Set<String> allowedConfigurableKeys(Role role) {
		Set<String> configurableKeys = MenuPage.configurablePages()
			.stream()
			.map(MenuPage::getKey)
			.collect(Collectors.toSet());
		return repository.findByRole(role)
			.stream()
			.map(RolePermission::getPageKey)
			.filter(configurableKeys::contains)
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}

}
