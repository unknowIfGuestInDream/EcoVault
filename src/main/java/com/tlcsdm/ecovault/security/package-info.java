/**
 * 安全配置包。
 *
 * <p>
 * 本包提供 EcoVault 应用的安全配置，包括 Spring Security 配置、JWT 工具、认证过滤器等。
 * </p>
 *
 * <h2>主要组件</h2>
 * <ul>
 *   <li>Spring Security 配置 - 配置安全规则、认证授权</li>
 *   <li>{@link com.tlcsdm.ecovault.security.JwtUtil} - JWT 工具类，生成和验证令牌</li>
 *   <li>{@link com.tlcsdm.ecovault.security.JwtAuthenticationFilter} - JWT 认证过滤器</li>
 *   <li>{@link com.tlcsdm.ecovault.security.CustomUserDetailsService} - 自定义用户详情服务</li>
 * </ul>
 *
 * <h2>安全策略</h2>
 * <ul>
 *   <li><b>认证</b>: 使用 JWT 令牌进行无状态认证</li>
 *   <li><b>授权</b>: 基于角色的访问控制（RBAC），角色包括 USER 和 ADMIN</li>
 *   <li><b>密码存储</b>: 使用 BCrypt 哈希算法，不存明文密码</li>
 *   <li><b>会话管理</b>: 支持单设备或多设备登录（通过配置 ecovault.security.max-devices）</li>
 *   <li><b>敏感数据加密</b>: 密码条目中的密码字段使用 AES-GCM 加密</li>
 * </ul>
 *
 * <h2>端点保护</h2>
 * <ul>
 *   <li>公开端点: /auth/login, /auth/logout（无需认证）</li>
 *   <li>用户端点: /password/**, /salary/**, /ledger/** 等（需要 USER 或 ADMIN 角色）</li>
 *   <li>管理端点: /admin/**, /actuator/** 等（仅 ADMIN 角色）</li>
 * </ul>
 *
 * @see com.tlcsdm.ecovault.config
 * @author unknowIfGuestInDream
 * @version 1.0.1
 * @since 1.0.0
 */
package com.tlcsdm.ecovault.security;
