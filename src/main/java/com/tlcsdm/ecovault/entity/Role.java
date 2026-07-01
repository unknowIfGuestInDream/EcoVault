package com.tlcsdm.ecovault.entity;

/**
 * 系统角色枚举 (RBAC)。
 *
 * <ul>
 * <li>{@link #USER} 普通用户</li>
 * <li>{@link #ADMIN} 管理员 (即开发者，可访问管理后台与 actuator)</li>
 * </ul>
 *
 * @author unknowIfGuestInDream
 */
public enum Role {

	/** 普通用户 */
	USER,

	/** 管理员 */
	ADMIN

}
