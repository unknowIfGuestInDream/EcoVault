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
 * @since 1.0.0
 * @see RolePermissionService
 */
@Service
/**
 * 角色权限服务实现类，负责维护角色与页面之间的授权关系。 支持后台权限矩阵查询和更新。
 *
 * @author unknowIfGuestInDream
 * @since 1.0.0
 * @see RolePermissionService
 */
public class RolePermissionServiceImpl implements RolePermissionService {

	private final RolePermissionRepository repository;

	/**
	 * 构造RolePermissionServiceImpl实例并注入所需依赖。
	 * @param repository repository参数。
	 */
	public RolePermissionServiceImpl(RolePermissionRepository repository) {
		this.repository = repository;
	}

	/**
	 * 处理initDefaults相关业务。
	 */
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

	/**
	 * 获取相关属性值。
	 * @return 方法执行结果。
	 */
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

	/**
	 * 更新已有业务数据。
	 * @param role 角色标识。
	 * @param pageKeys pageKeys参数。
	 */
	@Override
	@Transactional
	public void updatePermissions(Role role, List<String> pageKeys) {
		if (role == Role.ADMIN) {
			throw new BusinessException("ADMIN 角色默认拥有全部页面访问权限，不允许修改");
		}
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

	/**
	 * 处理accessiblePageKeys相关业务。
	 * @param user 用户实体。
	 * @return 方法执行结果。
	 */
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

	/**
	 * 处理canAccessPath相关业务。
	 * @param user 用户实体。
	 * @param path path参数。
	 * @return 方法执行结果。
	 */
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
