package com.tlcsdm.ecovault.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 密码条目实体。
 *
 * <p>用于存储用户的账号密码信息。其中 {@code secret} (密码本身) 与 {@code notes}
 * 在落库前使用 AES 加密，读取时解密，确保数据库中不保存明文。
 * {@code tags} 用于标签筛选，同样以加密形式存储，避免包含敏感关键字时被 Web 防火墙拦截。</p>
 *
 * @author unknowIfGuestInDream
 */
@Entity
@Table(name = "password_entries", indexes = {
        @Index(name = "idx_pwd_user", columnList = "user_id"),
        @Index(name = "idx_pwd_category", columnList = "category")
})
public class PasswordEntry {

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属用户 ID */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 条目标题 (如：GitHub 账号) */
    @Column(nullable = false, length = 128)
    private String title;

    /** 登录用户名/账号 */
    @Column(length = 128)
    private String account;

    /** 密码密文 (AES 加密) */
    @Lob
    @Column(nullable = false)
    private String secret;

    /** 站点地址 */
    @Column(length = 256)
    private String url;

    /** 备注密文 (AES 加密) */
    @Lob
    private String notes;

    /** 分类 */
    @Column(length = 64)
    private String category;

    /** 标签密文 (AES 加密，明文为英文逗号分隔) */
    @Column(length = 512)
    private String tags;

    /** 密码强度评分 (0-100) */
    @Column(name = "strength_score")
    private int strengthScore;

    /** 密码强度等级 (WEAK/MEDIUM/STRONG) */
    @Column(name = "strength_level", length = 16)
    private String strengthLevel;

    /** 创建时间 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public int getStrengthScore() {
        return strengthScore;
    }

    public void setStrengthScore(int strengthScore) {
        this.strengthScore = strengthScore;
    }

    public String getStrengthLevel() {
        return strengthLevel;
    }

    public void setStrengthLevel(String strengthLevel) {
        this.strengthLevel = strengthLevel;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
