package com.tlcsdm.ecovault.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 校验当前登录密码请求。
 *
 * <p>
 * 用于隐私模式解锁：网页进入隐私模式后，需再次输入当前账户登录密码才能继续访问。 该校验仅验证密码正确性，不重新签发 JWT，也不刷新会话，与令牌过期刷新逻辑相互独立。
 * </p>
 *
 * @param password 当前账户登录密码
 * @author unknowIfGuestInDream
 */
public record VerifyPasswordRequest(@NotBlank(message = "密码不能为空") String password) {
}
