/**
 * JPA 实体包。
 *
 * <p>
 * 本包提供 EcoVault 应用的 JPA 实体类，对应数据库表结构。
 * </p>
 *
 * <h2>主要实体类</h2>
 * <ul>
 * <li>{@link com.tlcsdm.ecovault.entity.User} - 用户实体</li>
 * <li>{@link com.tlcsdm.ecovault.entity.Session} - 会话实体</li>
 * <li>{@link com.tlcsdm.ecovault.entity.Password} - 密码条目实体</li>
 * <li>{@link com.tlcsdm.ecovault.entity.Salary} - 工资数据实体</li>
 * <li>{@link com.tlcsdm.ecovault.entity.Ledger} - 账本实体</li>
 * <li>{@link com.tlcsdm.ecovault.entity.OperationLog} - 操作日志实体</li>
 * </ul>
 *
 * <h2>设计原则</h2>
 * <ul>
 * <li>使用 JPA 注解定义表结构和字段映射</li>
 * <li>敏感字段（如密码）加密存储</li>
 * <li>审计字段（创建时间、更新时间）自动管理</li>
 * <li>外键关联使用 @ManyToOne、@OneToMany 等注解</li>
 * </ul>
 *
 * <h2>数据库</h2>
 * <p>
 * EcoVault 使用 SQLite 嵌入式数据库，数据文件默认存储于 {@code data/ecovault.db}。
 * </p>
 *
 * @see com.tlcsdm.ecovault.repository
 * @author unknowIfGuestInDream
 * @version 1.0.1
 * @since 1.0.0
 */
package com.tlcsdm.ecovault.entity;
