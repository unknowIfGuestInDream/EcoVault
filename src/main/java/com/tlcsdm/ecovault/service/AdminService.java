package com.tlcsdm.ecovault.service;

import com.tlcsdm.ecovault.dto.AdminUserResponse;

import java.util.List;

/**
 * 管理后台服务 (仅管理员)。
 *
 * @author unknowIfGuestInDream
 */
public interface AdminService {

    /**
     * 查询全部用户列表。
     *
     * @return 用户列表
     */
    List<AdminUserResponse> listUsers();

    /**
     * 启用或禁用用户。
     *
     * @param userId  用户 ID
     * @param enabled 是否启用
     */
    void setUserEnabled(Long userId, boolean enabled);
}
