package com.tlcsdm.ecovault.dto;

import java.util.List;

/**
 * 更新角色页面权限请求。
 *
 * @param pages 授权的可配置页面 key 列表
 * @author unknowIfGuestInDream
 * @since 1.0.0
 */
public record UpdateRolePermissionRequest(List<String> pages) {
}
