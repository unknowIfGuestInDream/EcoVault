/**
 * 业务服务实现包。
 *
 * <p>
 * 本包提供 EcoVault 应用的业务服务接口实现类。
 * </p>
 *
 * <h2>主要实现类</h2>
 * <ul>
 * <li>{@link com.tlcsdm.ecovault.service.impl.AuthServiceImpl} - 认证服务实现</li>
 * <li>{@link com.tlcsdm.ecovault.service.impl.PasswordServiceImpl} - 密码管理服务实现</li>
 * <li>{@link com.tlcsdm.ecovault.service.impl.SalaryServiceImpl} - 工资管理服务实现</li>
 * <li>{@link com.tlcsdm.ecovault.service.impl.LedgerServiceImpl} - 账本管理服务实现</li>
 * <li>{@link com.tlcsdm.ecovault.service.impl.OperationLogServiceImpl} - 操作日志服务实现</li>
 * <li>{@link com.tlcsdm.ecovault.service.impl.AdminServiceImpl} - 管理服务实现</li>
 * <li>{@link com.tlcsdm.ecovault.service.impl.RolePermissionServiceImpl} - 角色权限服务实现</li>
 * </ul>
 *
 * <h2>实现规范</h2>
 * <ul>
 * <li>使用 @Service 注解标记服务类</li>
 * <li>使用 @Transactional 注解管理事务</li>
 * <li>注入 Repository 进行数据访问</li>
 * <li>处理业务异常并返回有意义的错误信息</li>
 * </ul>
 *
 * @see com.tlcsdm.ecovault.service
 * @author unknowIfGuestInDream
 * @version 1.0.1
 * @since 1.0.0
 */
package com.tlcsdm.ecovault.service.impl;
