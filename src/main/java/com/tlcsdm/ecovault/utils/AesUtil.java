package com.tlcsdm.ecovault.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES-GCM 加解密工具。
 *
 * <p>用于对密码条目中的敏感字段 (密码、备注、标签) 进行加密存储，
 * 确保数据库中不保存明文，即使数据库泄露也难以还原。</p>
 *
 * @author unknowIfGuestInDream
 */
@Component
public class AesUtil {

    /** GCM 推荐的初始化向量长度 (字节) */
    private static final int IV_LENGTH = 12;

    /** GCM 认证标签长度 (位) */
    private static final int TAG_LENGTH_BIT = 128;

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private final SecretKeySpec keySpec;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 构造加密工具，从配置读取密钥并规整为 32 字节 (AES-256)。
     *
     * @param secret 配置的密钥
     */
    public AesUtil(@Value("${ecovault.crypto.secret}") String secret) {
        byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
        // 将密钥规整为 32 字节，不足补零，超出截断
        byte[] key = Arrays.copyOf(raw, 32);
        this.keySpec = new SecretKeySpec(key, "AES");
    }

    /**
     * 加密明文。
     *
     * @param plainText 明文 (可为 null)
     * @return Base64 编码的密文；若入参为 null 则返回 null
     */
    public String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            // 将 IV 与密文拼接后统一 Base64 编码
            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("加密失败", e);
        }
    }

    /**
     * 解密密文。
     *
     * @param cipherText Base64 编码的密文 (可为 null)
     * @return 明文；若入参为 null 则返回 null
     */
    public String decrypt(String cipherText) {
        if (cipherText == null) {
            return null;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(cipherText);
            byte[] iv = Arrays.copyOfRange(combined, 0, IV_LENGTH);
            byte[] actual = Arrays.copyOfRange(combined, IV_LENGTH, combined.length);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            byte[] plain = cipher.doFinal(actual);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("解密失败", e);
        }
    }
}
