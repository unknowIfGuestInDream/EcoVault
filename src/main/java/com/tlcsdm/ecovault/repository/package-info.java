/**
 * 数据访问层包。
 *
 * <p>
 * 本包提供 EcoVault 应用的 JPA Repository 接口，用于数据库操作。
 * </p>
 *
 * <h2>主要 Repository 接口</h2>
 * <ul>
 * <li>{@link com.tlcsdm.ecovault.repository.UserRepository} - 用户数据访问</li>
 * <li>{@link com.tlcsdm.ecovault.repository.SessionRepository} - 会话数据访问</li>
 * <li>{@link com.tlcsdm.ecovault.repository.PasswordRepository} - 密码条目数据访问</li>
 * <li>{@link com.tlcsdm.ecovault.repository.SalaryRepository} - 工资数据访问</li>
 * <li>{@link com.tlcsdm.ecovault.repository.LedgerRepository} - 账本数据访问</li>
 * <li>{@link com.tlcsdm.ecovault.repository.OperationLogRepository} - 操作日志数据访问</li>
 * </ul>
 *
 * <h2>设计原则</h2>
 * <ul>
 * <li>继承 {@code JpaRepository} 接口，获得基本 CRUD 方法</li>
 * <li>使用 {@code @Query} 注解定义自定义查询</li>
 * <li>使用参数绑定防止 SQL 注入</li>
 * <li>分页查询使用 {@code Pageable} 参数</li>
 * </ul>
 *
 * @see com.tlcsdm.ecovault.entity
 * @see com.tlcsdm.ecovault.service
 * @author unknowIfGuestInDream
 * @version 1.0.1
 * @since 1.0.0
 */
package com.tlcsdm.ecovault.repository;
