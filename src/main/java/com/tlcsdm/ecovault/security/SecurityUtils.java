package com.tlcsdm.ecovault.security;

import com.tlcsdm.ecovault.common.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全上下文工具，便于在业务层获取当前登录用户信息。
 *
 * @author unknowIfGuestInDream
 */
public final class SecurityUtils {

	private SecurityUtils() {
	}

	/**
	 * 获取当前登录用户主体。
	 * @return 当前用户主体
	 * @throws BusinessException 未登录时抛出
	 */
	public static SecurityUser getCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof SecurityUser securityUser) {
			return securityUser;
		}
		throw new BusinessException(401, "用户未登录");
	}

	/**
	 * 获取当前登录用户 ID。
	 * @return 用户 ID
	 */
	public static Long getCurrentUserId() {
		return getCurrentUser().getId();
	}

	/**
	 * 获取当前登录用户名。
	 * @return 用户名
	 */
	public static String getCurrentUsername() {
		return getCurrentUser().getUsername();
	}

	/**
	 * 判断当前用户是否为管理员。
	 * @return 是否为管理员
	 */
	public static boolean isAdmin() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication != null
				&& authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
	}

}
