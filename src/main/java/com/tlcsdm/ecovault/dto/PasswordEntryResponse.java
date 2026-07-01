package com.tlcsdm.ecovault.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 密码条目响应 (敏感字段已解密)。
 *
 * @param id            条目 ID
 * @param title         标题
 * @param account       账号
 * @param secret        明文密码
 * @param url           站点地址
 * @param notes         备注
 * @param category      分类
 * @param tags          标签列表
 * @param strengthScore 密码强度评分
 * @param strengthLevel 密码强度等级
 * @param createdAt     创建时间
 * @param updatedAt     更新时间
 * @author 梦里不知身是客
 */
public record PasswordEntryResponse(
        Long id,
        String title,
        String account,
        String secret,
        String url,
        String notes,
        String category,
        List<String> tags,
        int strengthScore,
        String strengthLevel,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
