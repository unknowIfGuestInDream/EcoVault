package com.tlcsdm.ecovault.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JWT 令牌提供者单元测试。
 *
 * @author 梦里不知身是客
 */
class JwtTokenProviderTest {

    private final JwtTokenProvider provider =
            new JwtTokenProvider("test-secret-key-for-unit-tests", 7200000L);

    @Test
    @DisplayName("生成的令牌可被解析并还原声明")
    void generateAndParse() {
        String jti = provider.newJti();
        String token = provider.generateToken("alice", 42L, jti);

        Claims claims = provider.parse(token);
        assertThat(claims).isNotNull();
        assertThat(claims.getSubject()).isEqualTo("alice");
        assertThat(claims.getId()).isEqualTo(jti);
        assertThat(claims.get("uid", Number.class).longValue()).isEqualTo(42L);
    }

    @Test
    @DisplayName("被篡改的令牌解析返回 null")
    void tamperedTokenReturnsNull() {
        String token = provider.generateToken("bob", 1L, provider.newJti());
        String tampered = token.substring(0, token.length() - 2) + "xx";
        assertThat(provider.parse(tampered)).isNull();
    }

    @Test
    @DisplayName("使用不同密钥签发的令牌校验失败")
    void differentKeyFails() {
        JwtTokenProvider other = new JwtTokenProvider("another-secret", 7200000L);
        String token = other.generateToken("carol", 2L, other.newJti());
        assertThat(provider.parse(token)).isNull();
    }

    @Test
    @DisplayName("newJti 生成不含连字符的唯一标识")
    void newJtiFormat() {
        String jti = provider.newJti();
        assertThat(jti).doesNotContain("-").hasSize(32);
    }
}
