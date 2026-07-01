package com.tlcsdm.ecovault.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AES-GCM 加解密工具单元测试。
 *
 * @author 梦里不知身是客
 */
class AesUtilTest {

    private final AesUtil aesUtil = new AesUtil("ecovault-test-aes-secret-32bytes!");

    @Test
    @DisplayName("加密后可正确解密还原明文")
    void encryptThenDecrypt() {
        String plain = "P@ssw0rd-机密信息";
        String cipher = aesUtil.encrypt(plain);
        assertThat(cipher).isNotNull().isNotEqualTo(plain);
        assertThat(aesUtil.decrypt(cipher)).isEqualTo(plain);
    }

    @Test
    @DisplayName("相同明文两次加密结果不同 (随机 IV)")
    void encryptionUsesRandomIv() {
        String plain = "same-text";
        assertThat(aesUtil.encrypt(plain)).isNotEqualTo(aesUtil.encrypt(plain));
    }

    @Test
    @DisplayName("null 入参返回 null")
    void nullPassthrough() {
        assertThat(aesUtil.encrypt(null)).isNull();
        assertThat(aesUtil.decrypt(null)).isNull();
    }

    @Test
    @DisplayName("非法密文解密抛出异常")
    void decryptInvalidThrows() {
        assertThatThrownBy(() -> aesUtil.decrypt("not-a-valid-cipher"))
                .isInstanceOf(IllegalStateException.class);
    }
}
