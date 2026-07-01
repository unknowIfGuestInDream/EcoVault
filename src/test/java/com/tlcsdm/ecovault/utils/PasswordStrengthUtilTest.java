package com.tlcsdm.ecovault.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 密码强度检测工具单元测试。
 *
 * @author unknowIfGuestInDream
 */
class PasswordStrengthUtilTest {

    @Test
    @DisplayName("空密码或 null 返回 WEAK 且分值为 0")
    void emptyPasswordIsWeak() {
        assertThat(PasswordStrengthUtil.evaluate(null).level()).isEqualTo("WEAK");
        assertThat(PasswordStrengthUtil.evaluate(null).score()).isZero();
        assertThat(PasswordStrengthUtil.evaluate("").level()).isEqualTo("WEAK");
    }

    @Test
    @DisplayName("简单短密码判定为 WEAK")
    void shortSimplePasswordIsWeak() {
        PasswordStrengthUtil.Strength strength = PasswordStrengthUtil.evaluate("abc");
        assertThat(strength.level()).isEqualTo("WEAK");
        assertThat(strength.score()).isLessThan(40);
    }

    @Test
    @DisplayName("中等复杂度密码判定为 MEDIUM")
    void mediumPassword() {
        // 8 位 (25) + 小写 (15) = 40 -> MEDIUM
        PasswordStrengthUtil.Strength strength = PasswordStrengthUtil.evaluate("abcdefgh");
        assertThat(strength.level()).isEqualTo("MEDIUM");
    }

    @Test
    @DisplayName("长且包含多种字符的密码判定为 STRONG")
    void strongPassword() {
        PasswordStrengthUtil.Strength strength = PasswordStrengthUtil.evaluate("Abcdef123!@#");
        assertThat(strength.level()).isEqualTo("STRONG");
        assertThat(strength.score()).isGreaterThanOrEqualTo(70);
    }

    @Test
    @DisplayName("评分上限不超过 100")
    void scoreCappedAt100() {
        PasswordStrengthUtil.Strength strength = PasswordStrengthUtil.evaluate("Abcdefghijkl123!@#$%");
        assertThat(strength.score()).isLessThanOrEqualTo(100);
    }
}
