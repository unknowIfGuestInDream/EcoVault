package com.tlcsdm.ecovault.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 令牌提供者，负责令牌的生成与解析校验。
 *
 * @author unknowIfGuestInDream
 */
@Component
public class JwtTokenProvider {

	private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

	private final SecretKey key;

	private final long expirationMs;

	/**
	 * 构造 JWT 提供者。
	 *
	 * <p>
	 * 对配置的密钥做 SHA-256 派生，确保得到稳定的 32 字节密钥满足 HS256 要求。
	 * </p>
	 * @param secret 签名密钥
	 * @param expirationMs 令牌有效期 (毫秒)
	 */
	public JwtTokenProvider(@Value("${ecovault.security.jwt-secret}") String secret,
			@Value("${ecovault.security.jwt-expiration-ms}") long expirationMs) {
		this.key = Keys.hmacShaKeyFor(sha256(secret));
		if (expirationMs <= 0) {
			throw new IllegalArgumentException("JWT 有效期必须大于 0");
		}
		this.expirationMs = expirationMs;
	}

	private static byte[] sha256(String input) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return digest.digest(input.getBytes(StandardCharsets.UTF_8));
		}
		catch (Exception e) {
			throw new IllegalStateException("无法初始化 JWT 密钥", e);
		}
	}

	/**
	 * 生成 JWT。
	 * @param username 用户名
	 * @param userId 用户 ID
	 * @param jti 会话唯一标识
	 * @return 签名后的 JWT 字符串
	 */
	public String generateToken(String username, Long userId, String jti) {
		Date now = new Date();
		Date expiry = new Date(now.getTime() + expirationMs);
		return Jwts.builder()
			.subject(username)
			.id(jti)
			.claim("uid", userId)
			.issuedAt(now)
			.expiration(expiry)
			.signWith(key)
			.compact();
	}

	/**
	 * 生成新的会话唯一标识。
	 * @return jti
	 */
	public String newJti() {
		return UUID.randomUUID().toString().replace("-", "");
	}

	/**
	 * 解析并校验令牌。
	 * @param token JWT 字符串
	 * @return 声明集合；校验失败返回 null
	 */
	public Claims parse(String token) {
		try {
			return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
		}
		catch (JwtException | IllegalArgumentException e) {
			log.debug("JWT 校验失败: {}", e.getMessage());
			return null;
		}
	}

	/**
	 * 返回当前 JWT 有效期配置。
	 * @return 有效期（毫秒）
	 */
	public long getExpirationMs() {
		return expirationMs;
	}

}
