/**
 * 数据传输对象包。
 *
 * <p>
 * 本包提供 EcoVault 应用的 DTO（Data Transfer Object）类，用于控制器与服务层之间的数据传输。
 * </p>
 *
 * <h2>主要 DTO 类</h2>
 * <ul>
 * <li>请求 DTO - 用于接收客户端请求参数</li>
 * <li>响应 DTO - 用于返回数据给客户端</li>
 * </ul>
 *
 * <h2>请求 DTO</h2>
 * <ul>
 * <li>{@link com.tlcsdm.ecovault.dto.LoginRequest} - 登录请求</li>
 * <li>{@link com.tlcsdm.ecovault.dto.PasswordRequest} - 密码条目请求</li>
 * <li>{@link com.tlcsdm.ecovault.dto.SalaryRequest} - 工资数据请求</li>
 * <li>{@link com.tlcsdm.ecovault.dto.LedgerRequest} - 账本请求</li>
 * <li>{@link com.tlcsdm.ecovault.dto.CreateUserRequest} - 创建用户请求</li>
 * <li>{@link com.tlcsdm.ecovault.dto.UpdateUserRequest} - 更新用户请求</li>
 * <li>{@link com.tlcsdm.ecovault.dto.VerifyPasswordRequest} - 验证密码请求</li>
 * </ul>
 *
 * <h2>响应 DTO</h2>
 * <ul>
 * <li>{@link com.tlcsdm.ecovault.dto.LoginResponse} - 登录响应</li>
 * <li>{@link com.tlcsdm.ecovault.dto.PasswordResponse} - 密码条目响应</li>
 * <li>{@link com.tlcsdm.ecovault.dto.SalaryResponse} - 工资数据响应</li>
 * <li>{@link com.tlcsdm.ecovault.dto.LedgerResponse} - 账本响应</li>
 * <li>{@link com.tlcsdm.ecovault.dto.AdminUserResponse} - 管理后台用户响应</li>
 * </ul>
 *
 * <h2>设计原则</h2>
 * <ul>
 * <li>DTO 与 Entity 分离，避免直接暴露数据库结构</li>
 * <li>使用 Bean Validation 注解进行参数校验</li>
 * <li>敏感字段不包含在响应 DTO 中（如密码）</li>
 * </ul>
 *
 * @see com.tlcsdm.ecovault.controller
 * @see com.tlcsdm.ecovault.entity
 * @author unknowIfGuestInDream
 * @version 1.0.1
 * @since 1.0.0
 */
package com.tlcsdm.ecovault.dto;
