/**
 * Spring 配置包。
 *
 * <p>
 * 本包提供 EcoVault 应用的 Spring 配置类，包括 Web 配置、数据库配置、安全配置等。
 * </p>
 *
 * <h2>主要配置类</h2>
 * <ul>
 * <li>Web MVC 配置 - 配置拦截器、消息转换器、跨域等</li>
 * <li>数据源配置 - 配置 SQLite 数据源和 JPA</li>
 * <li>安全配置 - 配置 Spring Security 和 JWT</li>
 * <li>AOP 配置 - 配置切面代理</li>
 * </ul>
 *
 * <h2>Bean 定义</h2>
 * <p>
 * 配置类定义了应用所需的各种 Bean，如 PasswordEncoder、JwtUtil、加密工具等。
 * </p>
 *
 * @see com.tlcsdm.ecovault.security
 * @author unknowIfGuestInDream
 * @version 1.0.1
 * @since 1.0.0
 */
package com.tlcsdm.ecovault.config;
