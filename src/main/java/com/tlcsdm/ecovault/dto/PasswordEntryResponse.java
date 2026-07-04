package com.tlcsdm.ecovault.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 密码条目响应。
 *
 * <p>
 * 详情/新增/编辑接口返回解密后的敏感字段；列表接口可按需对密码等字段做脱敏处理。
 * </p>
 *
 * @param id 条目 ID
 * @param title 标题
 * @param account 账号
 * @param secret 明文密码
 * @param url 站点地址
 * @param notes 备注
 * @param category 分类
 * @param tags 标签列表
 * @param strengthScore 密码强度评分
 * @param strengthLevel 密码强度等级
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 * @author unknowIfGuestInDream
 */
public record PasswordEntryResponse(Long id, String title, String account, String secret, String url, String notes,
		String category, List<String> tags, int strengthScore, String strengthLevel, LocalDateTime createdAt,
		LocalDateTime updatedAt) {
}
