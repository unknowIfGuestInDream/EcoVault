/**
 * Web 控制器包。
 *
 * <p>
 * 本包提供 EcoVault 应用的 Web 控制器，负责处理 HTTP 请求并返回响应。
 * </p>
 *
 * <h2>主要控制器</h2>
 * <ul>
 *   <li>{@link com.tlcsdm.ecovault.controller.AuthController} - 认证控制器，处理登录、登出</li>
 *   <li>{@link com.tlcsdm.ecovault.controller.PasswordController} - 密码管理控制器</li>
 *   <li>{@link com.tlcsdm.ecovault.controller.SalaryController} - 工资管理控制器</li>
 *   <li>{@link com.tlcsdm.ecovault.controller.LedgerController} - 账本管理控制器</li>
 *   <li>{@link com.tlcsdm.ecovault.controller.LogController} - 日志查询控制器</li>
 *   <li>{@link com.tlcsdm.ecovault.controller.AdminController} - 管理后台控制器</li>
 *   <li>{@link com.tlcsdm.ecovault.controller.PageController} - 页面渲染控制器</li>
 * </ul>
 *
 * <h2>设计原则</h2>
 * <ul>
 *   <li>RESTful API 设计规范</li>
 *   <li>统一的异常处理</li>
 *   <li>参数校验与绑定</li>
 *   <li>权限控制与认证</li>
 * </ul>
 *
 * @see com.tlcsdm.ecovault.service
 * @see com.tlcsdm.ecovault.dto
 * @author unknowIfGuestInDream
 * @version 1.0.1
 * @since 1.0.0
 */
package com.tlcsdm.ecovault.controller;
