package com.setpik.server.auth.domain;

import com.setpik.server.common.domain.CreatedAtEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** Flyway의 Auth_Refresh_Tokens 테이블을 그대로 매핑한다. */
@Entity
@Table(name = "Auth_Refresh_Tokens")
public class AuthRefreshToken extends CreatedAtEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "refresh_token_id", nullable = false)
	private Long refreshTokenId;

	@Column(name = "token_hash", nullable = false, length = 255)
	private String tokenHash;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	@Column(name = "revoked_at", nullable = true)
	private LocalDateTime revokedAt;

	@Column(name = "last_used_at", nullable = true)
	private LocalDateTime lastUsedAt;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	protected AuthRefreshToken() {
	}

	/** 쿠키 원문 대신 해시만 저장하는 SetPIK Refresh Token을 발급한다. */
	public static AuthRefreshToken issue(
		String tokenHash,
		LocalDateTime expiresAt,
		Long userId
	) {
		AuthRefreshToken refreshToken = new AuthRefreshToken();
		refreshToken.tokenHash = tokenHash;
		refreshToken.expiresAt = expiresAt;
		refreshToken.userId = userId;
		return refreshToken;
	}

	public Long getRefreshTokenId() {
		return refreshTokenId;
	}

	public String getTokenHash() {
		return tokenHash;
	}

	public LocalDateTime getExpiresAt() {
		return expiresAt;
	}

	public LocalDateTime getRevokedAt() {
		return revokedAt;
	}

	public LocalDateTime getLastUsedAt() {
		return lastUsedAt;
	}

	public Long getUserId() {
		return userId;
	}

}
