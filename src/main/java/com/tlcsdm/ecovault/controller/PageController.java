package com.tlcsdm.ecovault.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 页面路由控制器 (Thymeleaf 服务端渲染)。
 *
 * <p>仅负责返回视图名称，具体数据通过前端 JS 调用 REST 接口获取。
 * 受保护页面的访问控制由 Spring Security 统一处理。</p>
 */
@Controller
public class PageController {

    /**
     * 首页。
     *
     * @return 视图名
     */
    @GetMapping("/")
    public String index() {
        return "index";
    }

    /**
     * 登录页。
     *
     * @return 视图名
     */
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /**
     * 控制台首页。
     *
     * @return 视图名
     */
    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    /**
     * 密码管理页。
     *
     * @return 视图名
     */
    @GetMapping("/passwords")
    public String passwords() {
        return "passwords";
    }

    /**
     * 财务管理页。
     *
     * @return 视图名
     */
    @GetMapping("/finance")
    public String finance() {
        return "finance";
    }

    /**
     * 日志管理页。
     *
     * @return 视图名
     */
    @GetMapping("/logs")
    public String logs() {
        return "logs";
    }

    /**
     * 个人中心页。
     *
     * @return 视图名
     */
    @GetMapping("/profile")
    public String profile() {
        return "profile";
    }

    /**
     * 管理后台页 (仅管理员)。
     *
     * @return 视图名
     */
    @GetMapping("/admin")
    public String admin() {
        return "admin";
    }
}
