package com.tlcsdm.ecovault.service;

import com.tlcsdm.ecovault.dto.PasswordEntryRequest;
import com.tlcsdm.ecovault.dto.PasswordEntryResponse;

import java.util.List;

/**
 * 密码管理服务。
 *
 * @author 梦里不知身是客
 */
public interface PasswordService {

    /**
     * 创建密码条目。
     *
     * @param userId  用户 ID
     * @param request 请求
     * @return 创建后的条目
     */
    PasswordEntryResponse create(Long userId, PasswordEntryRequest request);

    /**
     * 更新密码条目。
     *
     * @param userId  用户 ID
     * @param id      条目 ID
     * @param request 请求
     * @return 更新后的条目
     */
    PasswordEntryResponse update(Long userId, Long id, PasswordEntryRequest request);

    /**
     * 删除密码条目。
     *
     * @param userId 用户 ID
     * @param id     条目 ID
     */
    void delete(Long userId, Long id);

    /**
     * 查询单个条目详情。
     *
     * @param userId 用户 ID
     * @param id     条目 ID
     * @return 条目
     */
    PasswordEntryResponse get(Long userId, Long id);

    /**
     * 查询/搜索/按标签筛选条目。
     *
     * @param userId  用户 ID
     * @param keyword 标题关键字 (可空)
     * @param tag     标签 (可空)
     * @return 条目列表
     */
    List<PasswordEntryResponse> list(Long userId, String keyword, String tag);
}
