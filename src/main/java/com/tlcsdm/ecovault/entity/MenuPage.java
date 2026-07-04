package com.tlcsdm.ecovault.entity;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 系统菜单/页面枚举 (用于 RBAC 页面级访问控制)。
 *
 * <p>
 * 每个页面拥有唯一 {@code key}、展示名称、访问路径与所属菜单分组。角色管理中，管理员可为角色分配可访问的
 * 「可配置页面」({@link #isConfigurable()})；管理后台分组下的页面固定仅管理员可访问；控制台与个人中心对所有登录用户开放。
 * </p>
 *
 * @author unknowIfGuestInDream
 */
public enum MenuPage {

	/** 控制台 (所有登录用户) */
	DASHBOARD("dashboard", "控制台", "/dashboard", MenuGroup.MAIN, false, false),

	/** 密码管理 (可配置) */
	PASSWORDS("passwords", "密码管理", "/passwords", MenuGroup.MAIN, false, true),

	/** 财务 - 工资管理 (可配置) */
	SALARY("salary", "工资管理", "/finance", MenuGroup.FINANCE, false, true),

	/** 财务 - 收入支出管理 (可配置) */
	LEDGER("ledger", "收入支出管理", "/finance/ledger", MenuGroup.FINANCE, false, true),

	/** 个人中心 (所有登录用户) */
	PROFILE("profile", "个人中心", "/profile", MenuGroup.MAIN, false, false),

	/** 后台 - 用户管理 (仅管理员) */
	USERS("users", "用户管理", "/admin/users", MenuGroup.ADMIN, true, false),

	/** 后台 - 日志管理 (仅管理员) */
	LOGS("logs", "日志管理", "/admin/logs", MenuGroup.ADMIN, true, false),

	/** 后台 - 角色管理 (仅管理员) */
	ROLES("roles", "角色管理", "/admin/roles", MenuGroup.ADMIN, true, false);

	/** 菜单分组 */
	public enum MenuGroup {

		/** 主菜单 */
		MAIN,

		/** 财务管理 */
		FINANCE,

		/** 后台管理 */
		ADMIN

	}

	private final String key;

	private final String label;

	private final String path;

	private final MenuGroup group;

	private final boolean adminOnly;

	private final boolean configurable;

	MenuPage(String key, String label, String path, MenuGroup group, boolean adminOnly, boolean configurable) {
		this.key = key;
		this.label = label;
		this.path = path;
		this.group = group;
		this.adminOnly = adminOnly;
		this.configurable = configurable;
	}

	public String getKey() {
		return key;
	}

	public String getLabel() {
		return label;
	}

	public String getPath() {
		return path;
	}

	public MenuGroup getGroup() {
		return group;
	}

	/**
	 * 是否为仅管理员可访问的后台页面。
	 * @return 是否仅管理员
	 */
	public boolean isAdminOnly() {
		return adminOnly;
	}

	/**
	 * 是否为可由角色管理配置访问权限的页面。
	 * @return 是否可配置
	 */
	public boolean isConfigurable() {
		return configurable;
	}

	/**
	 * 所有可配置页面 (可在角色管理中分配)。
	 * @return 可配置页面列表
	 */
	public static List<MenuPage> configurablePages() {
		return Arrays.stream(values()).filter(MenuPage::isConfigurable).toList();
	}

	/**
	 * 根据页面 key 查找枚举。
	 * @param key 页面 key
	 * @return 对应枚举 (可能为空)
	 */
	public static Optional<MenuPage> fromKey(String key) {
		return Arrays.stream(values()).filter(p -> p.key.equals(key)).findFirst();
	}

}
