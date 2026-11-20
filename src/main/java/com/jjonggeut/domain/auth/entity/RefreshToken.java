package com.jjonggeut.domain.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "refresh_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "refresh_token_id")
	private Long refreshTokenId;

	@Column(name = "user_id", nullable = false, unique = true)
	private Long userId;

	/** refresh token 원문이 아닌 SHA-256 해시를 저장한다. */
	@Column(name = "token_hash", nullable = false, length = 64, unique = true)
	private String tokenHash;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	public static RefreshToken create(Long userId, String tokenHash, LocalDateTime expiresAt) {
		RefreshToken token = new RefreshToken();
		token.userId = userId;
		token.tokenHash = tokenHash;
		token.expiresAt = expiresAt;
		return token;
	}

	public void rotate(String tokenHash, LocalDateTime expiresAt) {
		this.tokenHash = tokenHash;
		this.expiresAt = expiresAt;
	}
}
