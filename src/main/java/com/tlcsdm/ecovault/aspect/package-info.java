/**
 * AOP 切面包。
 *
 * <p>
 * 本包提供 EcoVault 应用的 AOP 切面实现，用于实现横切关注点。
 * </p>
 *
 * <h2>主要切面</h2>
 * <ul>
 *   <li>{@link com.tlcsdm.ecovault.aspect.OperationLogAspect} - 操作日志切面，自动记录关键操作日志</li>
 * </ul>
 *
 * <h2>功能说明</h2>
 * <p>
 * 操作日志切面通过拦截带有 {@link com.tlcsdm.ecovault.annotation.OperationLog} 注解的方法，
 * 自动提取用户信息、操作模块、操作动作、IP 地址等信息，并保存到数据库中。
 * </p>
 *
 * <h2>敏感信息脱敏</h2>
 * <p>
 * 切面会对日志中的敏感信息（如密码、令牌等）进行脱敏处理，确保日志安全。
 * </p>
 *
 * @see com.tlcsdm.ecovault.annotation.OperationLog
 * @see com.tlcsdm.ecovault.service.OperationLogService
 * @author unknowIfGuestInDream
 * @version 1.0.1
 * @since 1.0.0
 */
package com.tlcsdm.ecovault.aspect;
