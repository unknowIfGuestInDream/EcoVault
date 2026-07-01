package com.tlcsdm.ecovault.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 密码条目新增/编辑请求。
 *
 * @param title    标题
 * @param account  账号
 * @param secret   明文密码 (服务端 AES 加密后存储)
 * @param url      站点地址
 * @param notes    备注 (服务端 AES 加密后存储)
 * @param category 分类
 * @param tags     标签列表
 * @author unknowIfGuestInDream
 */
public record PasswordEntryRequest(
        @NotBlank(message = "标题不能为空")
        @Size(max = 128, message = "标题过长")
        String title,

        @Size(max = 128, message = "账号过长")
        String account,

        @NotBlank(message = "密码不能为空")
        String secret,

        @Size(max = 256, message = "站点地址过长")
        String url,

        String notes,

        @Size(max = 64, message = "分类过长")
        String category,

        List<String> tags
) {
}
