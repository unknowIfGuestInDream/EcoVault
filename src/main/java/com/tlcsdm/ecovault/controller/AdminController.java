package com.tlcsdm.ecovault.controller;

import com.tlcsdm.ecovault.annotation.OperationLogRecord;
import com.tlcsdm.ecovault.common.ApiResponse;
import com.tlcsdm.ecovault.dto.AdminUserResponse;
import com.tlcsdm.ecovault.service.AdminService;
import org.springframework.boot.info.BuildProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理后台接口 (仅管理员，路径受安全配置保护)。
 *
 * @author 梦里不知身是客
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    private final ObjectProvider<BuildProperties> buildProperties;

    public AdminController(AdminService adminService, ObjectProvider<BuildProperties> buildProperties) {
        this.adminService = adminService;
        this.buildProperties = buildProperties;
    }

    /**
     * 查询全部用户。
     *
     * @return 用户列表
     */
    @GetMapping("/users")
    public ApiResponse<List<AdminUserResponse>> listUsers() {
        return ApiResponse.success(adminService.listUsers());
    }

    /**
     * 启用/禁用用户。
     *
     * @param id      用户 ID
     * @param enabled 是否启用
     * @return 操作结果
     */
    @PutMapping("/users/{id}/status")
    @OperationLogRecord(module = "管理后台", operation = "修改用户状态")
    public ApiResponse<Void> setStatus(@PathVariable Long id, @RequestParam boolean enabled) {
        adminService.setUserEnabled(id, enabled);
        return ApiResponse.success();
    }

    /**
     * 获取系统构建信息 (供管理员/开发者查看当前部署版本)。
     *
     * @return 构建信息
     */
    @GetMapping("/build-info")
    public ApiResponse<Map<String, Object>> buildInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        BuildProperties props = buildProperties.getIfAvailable();
        if (props != null) {
            info.put("group", props.getGroup());
            info.put("artifact", props.getArtifact());
            info.put("name", props.getName());
            info.put("version", props.getVersion());
            info.put("buildTime", props.getTime() == null ? null : props.getTime().toString());
        } else {
            info.put("version", "开发环境 (未生成构建信息)");
        }
        info.put("javaVersion", System.getProperty("java.version"));
        return ApiResponse.success(info);
    }
}
