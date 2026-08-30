/**
 * 自定义注解包。
 *
 * <p>
 * 本包提供 EcoVault 应用的自定义注解，主要用于 AOP 切面和功能增强。
 * </p>
 *
 * <h2>主要注解</h2>
 * <ul>
 *   <li>{@link com.tlcsdm.ecovault.annotation.OperationLog} - 操作日志注解，用于标记需要记录日志的方法</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * @OperationLog(module = "密码管理", action = "新增密码")
 * public void createPassword(PasswordRequest request) {
 *     // 业务逻辑
 * }
 * }</pre>
 *
 * @see com.tlcsdm.ecovault.aspect.OperationLogAspect
 * @author unknowIfGuestInDream
 * @version 1.0.1
 * @since 1.0.0
 */
package com.tlcsdm.ecovault.annotation;
