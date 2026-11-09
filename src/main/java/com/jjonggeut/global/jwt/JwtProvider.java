package com.jjonggeut.global.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtProvider {

	private final SecretKey key;
	private final long accessExpirationSeconds;
	private final long refreshExpirationSeconds;

	public JwtProvider(
			@Value("${jwt.secret}") String secret,
			@Value("${jwt.access-expiration-seconds}") long accessExpirationSeconds,
			@Value("${jwt.refresh-expiration-seconds}") long refreshExpirationSeconds) {
		// 32바이트 미만이면 Keys.hmacShaKeyFor 에서 예외가 발생해 서버가 기동되지 않는다.
		this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.accessExpirationSeconds = accessExpirationSeconds;
		this.refreshExpirationSeconds = refreshExpirationSeconds;
	}

	public String createAccessToken(Long userId, String role) {
		Instant now = Instant.now();
		return Jwts.builder()
				.subject(String.valueOf(userId))
				.claim("role", role)
				.claim("type", "access")
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plusSeconds(accessExpirationSeconds)))
				.signWith(key)
				.compact();
	}

	public String createRefreshToken(Long userId) {
		Instant now = Instant.now();
		return Jwts.builder()
				.subject(String.valueOf(userId))
				.claim("type", "refresh")
				.id(UUID.randomUUID().toString())
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plusSeconds(refreshExpirationSeconds)))
				.signWith(key)
				.compact();
	}

	public long getAccessExpirationSeconds() {
		return accessExpirationSeconds;
	}

	public Duration getRefreshExpiration() {
		return Duration.ofSeconds(refreshExpirationSeconds);
	}
}
