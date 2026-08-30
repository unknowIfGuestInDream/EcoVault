/**
 * EcoVault 生态保险箱根包。
 *
 * <p>
 * 本包包含 EcoVault 应用的主入口类和核心启动逻辑。 EcoVault
 * 是一个个人数据安全存储与智能管理平台，提供用户管理、密码管理、财务管理、日志审计与管理后台等功能。
 * </p>
 *
 * <h2>项目特性</h2>
 * <ul>
 * <li>基于 Spring Boot 4 和 Java 25</li>
 * <li>使用 SQLite 嵌入式数据库</li>
 * <li>Thymeleaf 服务端渲染</li>
 * <li>Spring Security + JWT 认证授权</li>
 * <li>BCrypt 密码哈希 + AES-GCM 敏感数据加密</li>
 * <li>AOP 操作日志自动记录</li>
 * </ul>
 *
 * <h2>主要模块</h2>
 * <ul>
 * <li>{@link com.tlcsdm.ecovault.annotation} - 自定义注解</li>
 * <li>{@link com.tlcsdm.ecovault.aspect} - AOP 切面</li>
 * <li>{@link com.tlcsdm.ecovault.common} - 通用常量与枚举</li>
 * <li>{@link com.tlcsdm.ecovault.config} - Spring 配置</li>
 * <li>{@link com.tlcsdm.ecovault.controller} - Web 控制器</li>
 * <li>{@link com.tlcsdm.ecovault.dto} - 数据传输对象</li>
 * <li>{@link com.tlcsdm.ecovault.entity} - JPA 实体</li>
 * <li>{@link com.tlcsdm.ecovault.repository} - 数据访问层</li>
 * <li>{@link com.tlcsdm.ecovault.security} - 安全配置</li>
 * <li>{@link com.tlcsdm.ecovault.service} - 业务服务</li>
 * <li>{@link com.tlcsdm.ecovault.utils} - 工具类</li>
 * </ul>
 *
 * @author unknowIfGuestInDream
 * @version 1.0.1
 * @since 1.0.0
 */
package com.tlcsdm.ecovault;
