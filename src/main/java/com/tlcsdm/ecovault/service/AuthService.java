package com.tlcsdm.ecovault.service;

import com.tlcsdm.ecovault.dto.ChangePasswordRequest;
import com.tlcsdm.ecovault.dto.LoginRequest;
import com.tlcsdm.ecovault.dto.LoginResponse;
import com.tlcsdm.ecovault.dto.RegisterRequest;
import com.tlcsdm.ecovault.dto.UpdateProfileRequest;
import com.tlcsdm.ecovault.entity.User;

/**
 * 认证与用户信息服务。
 *
 * @author 梦里不知身是客
 */
public interface AuthService {

    /**
     * 用户注册。
     *
     * @param request 注册请求
     * @return 注册后的用户
     */
    User register(RegisterRequest request);

    /**
     * 用户登录。
     *
     * <p>登录成功后生成 JWT，并创建登录会话；若用户当前活跃会话数超过配置的最大设备数，
     * 则将最早的会话强制下线 (单设备登录限制)。</p>
     *
     * @param request    登录请求
     * @param deviceInfo 设备信息 (User-Agent)
     * @param ip         客户端 IP
     * @return 登录响应 (含 JWT)
     */
    LoginResponse login(LoginRequest request, String deviceInfo, String ip);

    /**
     * 注销当前会话。
     *
     * @param jti 会话唯一标识
     */
    void logout(String jti);

    /**
     * 修改个人信息。
     *
     * @param userId  用户 ID
     * @param request 修改请求
     * @return 更新后的用户
     */
    User updateProfile(Long userId, UpdateProfileRequest request);

    /**
     * 修改密码。
     *
     * @param userId  用户 ID
     * @param request 修改密码请求
     */
    void changePassword(Long userId, ChangePasswordRequest request);
}
