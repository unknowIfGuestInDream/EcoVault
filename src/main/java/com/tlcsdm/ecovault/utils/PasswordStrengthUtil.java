package com.tlcsdm.ecovault.utils;

/**
 * 密码强度检测工具。
 *
 * <p>综合长度、字符种类 (小写、大写、数字、特殊字符) 计算 0-100 的评分，
 * 并映射为 WEAK / MEDIUM / STRONG 三个等级。</p>
 *
 * @author unknowIfGuestInDream
 */
public final class PasswordStrengthUtil {

    private PasswordStrengthUtil() {
    }

    /** 密码强度评估结果 */
    public record Strength(int score, String level) {
    }

    /**
     * 评估密码强度。
     *
     * @param password 待评估密码
     * @return 强度评分与等级
     */
    public static Strength evaluate(String password) {
        if (password == null || password.isEmpty()) {
            return new Strength(0, "WEAK");
        }

        int score = 0;

        // 长度得分 (最多 40 分)
        int length = password.length();
        if (length >= 12) {
            score += 40;
        } else if (length >= 8) {
            score += 25;
        } else if (length >= 6) {
            score += 10;
        }

        // 字符种类得分 (每类 15 分)
        boolean lower = password.chars().anyMatch(Character::isLowerCase);
        boolean upper = password.chars().anyMatch(Character::isUpperCase);
        boolean digit = password.chars().anyMatch(Character::isDigit);
        boolean special = password.chars().anyMatch(c -> !Character.isLetterOrDigit(c));

        if (lower) {
            score += 15;
        }
        if (upper) {
            score += 15;
        }
        if (digit) {
            score += 15;
        }
        if (special) {
            score += 15;
        }

        if (score > 100) {
            score = 100;
        }

        String level;
        if (score >= 70) {
            level = "STRONG";
        } else if (score >= 40) {
            level = "MEDIUM";
        } else {
            level = "WEAK";
        }
        return new Strength(score, level);
    }
}
