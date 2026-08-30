/**
 * 业务服务包。
 *
 * <p>
 * 本包提供 EcoVault 应用的业务服务接口和实现类，负责核心业务逻辑处理。
 * </p>
 *
 * <h2>主要服务接口</h2>
 * <ul>
 * <li>{@link com.tlcsdm.ecovault.service.AuthService} - 认证服务，处理登录、登出、JWT 生成</li>
 * <li>{@link com.tlcsdm.ecovault.service.PasswordService} - 密码管理服务，处理密码条目的增删改查、加解密</li>
 * <li>{@link com.tlcsdm.ecovault.service.SalaryService} - 工资管理服务，处理工资数据的管理和统计</li>
 * <li>{@link com.tlcsdm.ecovault.service.LedgerService} - 账本管理服务，处理账本数据</li>
 * <li>{@link com.tlcsdm.ecovault.service.OperationLogService} - 操作日志服务，记录和查询操作日志</li>
 * <li>{@link com.tlcsdm.ecovault.service.AdminService} - 管理服务，处理用户管理、系统信息查询</li>
 * <li>{@link com.tlcsdm.ecovault.service.RolePermissionService} - 角色权限服务，处理权限校验</li>
 * </ul>
 *
 * <h2>服务实现</h2>
 * <p>
 * 服务实现类位于 {@link com.tlcsdm.ecovault.service.impl} 子包中。
 * </p>
 *
 * <h2>设计原则</h2>
 * <ul>
 * <li>接口与实现分离，便于测试和扩展</li>
 * <li>使用 @Transactional 注解管理事务</li>
 * <li>业务规则与数据访问分离</li>
 * <li>敏感操作进行权限校验</li>
 * </ul>
 *
 * @see com.tlcsdm.ecovault.service.impl
 * @see com.tlcsdm.ecovault.repository
 * @see com.tlcsdm.ecovault.controller
 * @author unknowIfGuestInDream
 * @version 1.0.1
 * @since 1.0.0
 */
package com.tlcsdm.ecovault.service;
