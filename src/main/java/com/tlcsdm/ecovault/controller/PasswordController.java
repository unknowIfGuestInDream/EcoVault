package com.tlcsdm.ecovault.controller;

import com.tlcsdm.ecovault.annotation.OperationLogRecord;
import com.tlcsdm.ecovault.common.ApiResponse;
import com.tlcsdm.ecovault.dto.PasswordEntryRequest;
import com.tlcsdm.ecovault.dto.PasswordEntryResponse;
import com.tlcsdm.ecovault.security.SecurityUtils;
import com.tlcsdm.ecovault.service.PasswordService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 密码管理接口。
 *
 * @author unknowIfGuestInDream
 */
@RestController
@RequestMapping("/api/passwords")
public class PasswordController {

    private final PasswordService passwordService;

    public PasswordController(PasswordService passwordService) {
        this.passwordService = passwordService;
    }

    /**
     * 查询/搜索/按标签筛选密码条目。
     *
     * @param keyword 标题关键字 (可空)
     * @param tag     标签 (可空)
     * @return 条目列表
     */
    @GetMapping
    public ApiResponse<List<PasswordEntryResponse>> list(@RequestParam(required = false) String keyword,
                                                         @RequestParam(required = false) String tag) {
        return ApiResponse.success(passwordService.list(SecurityUtils.getCurrentUserId(), keyword, tag));
    }

    /**
     * 查询单个条目详情。
     *
     * @param id 条目 ID
     * @return 条目
     */
    @GetMapping("/{id}")
    public ApiResponse<PasswordEntryResponse> get(@PathVariable Long id) {
        return ApiResponse.success(passwordService.get(SecurityUtils.getCurrentUserId(), id));
    }

    /**
     * 新增密码条目。
     *
     * @param request 请求
     * @return 新增结果
     */
    @PostMapping
    @OperationLogRecord(module = "密码管理", operation = "新增密码条目")
    public ApiResponse<PasswordEntryResponse> create(@Valid @RequestBody PasswordEntryRequest request) {
        return ApiResponse.success(passwordService.create(SecurityUtils.getCurrentUserId(), request));
    }

    /**
     * 更新密码条目。
     *
     * @param id      条目 ID
     * @param request 请求
     * @return 更新结果
     */
    @PutMapping("/{id}")
    @OperationLogRecord(module = "密码管理", operation = "更新密码条目")
    public ApiResponse<PasswordEntryResponse> update(@PathVariable Long id,
                                                     @Valid @RequestBody PasswordEntryRequest request) {
        return ApiResponse.success(passwordService.update(SecurityUtils.getCurrentUserId(), id, request));
    }

    /**
     * 删除密码条目。
     *
     * @param id 条目 ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    @OperationLogRecord(module = "密码管理", operation = "删除密码条目")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        passwordService.delete(SecurityUtils.getCurrentUserId(), id);
        return ApiResponse.success();
    }
}
