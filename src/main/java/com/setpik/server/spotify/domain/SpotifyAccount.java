package com.setpik.server.spotify.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** Flyway의 Spotify_accounts 테이블을 그대로 매핑한다. */
@Entity
@Table(name = "Spotify_accounts")
public class SpotifyAccount {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "spotify_account_id", nullable = false)
	private Long spotifyAccountId;

	@Column(name = "spotify_user_id", nullable = false, length = 255)
	private String spotifyUserId;

	@Column(name = "spotify_email", nullable = true, length = 255)
	private String spotifyEmail;

	@Column(name = "display_name", nullable = true, length = 255)
	private String displayName;

	@Column(name = "profile_image_url", nullable = true, length = 2048)
	private String profileImageUrl;

	@Column(name = "access_token_encrypted", nullable = true, columnDefinition = "TEXT")
	private String accessTokenEncrypted;

	@Column(name = "refresh_token_encrypted", nullable = true, columnDefinition = "TEXT")
	private String refreshTokenEncrypted;

	@Column(name = "token_expires_at", nullable = true)
	private LocalDateTime tokenExpiresAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "connection_status", nullable = false, length = 50)
	private ConnectionStatus connectionStatus;

	@Column(name = "connected_at", nullable = false)
	private LocalDateTime connectedAt;

	@Column(name = "disconnected_at", nullable = true)
	private LocalDateTime disconnectedAt;

	@Column(name = "last_profile_synced_at", nullable = true)
	private LocalDateTime lastProfileSyncedAt;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	protected SpotifyAccount() {
	}

	/** 최초 Spotify 로그인 정보를 내부 계정으로 생성한다. */
	public static SpotifyAccount connect(
		String spotifyUserId,
		String spotifyEmail,
		String displayName,
		String profileImageUrl,
		String accessTokenEncrypted,
		String refreshTokenEncrypted,
		LocalDateTime tokenExpiresAt,
		Long userId,
		LocalDateTime connectedAt
	) {
		SpotifyAccount account = new SpotifyAccount();
		account.spotifyUserId = spotifyUserId;
		account.spotifyEmail = spotifyEmail;
		account.displayName = displayName;
		account.profileImageUrl = profileImageUrl;
		account.accessTokenEncrypted = accessTokenEncrypted;
		account.refreshTokenEncrypted = refreshTokenEncrypted;
		account.tokenExpiresAt = tokenExpiresAt;
		account.connectionStatus = ConnectionStatus.CONNECTED;
		account.connectedAt = connectedAt;
		account.lastProfileSyncedAt = connectedAt;
		account.userId = userId;
		return account;
	}

	/** 재로그인 시 Spotify 프로필과 토큰 정보를 최신 상태로 갱신한다. */
	public void reconnect(
		String spotifyEmail,
		String displayName,
		String profileImageUrl,
		String accessTokenEncrypted,
		String refreshTokenEncrypted,
		LocalDateTime tokenExpiresAt,
		LocalDateTime connectedAt
	) {
		this.spotifyEmail = spotifyEmail;
		this.displayName = displayName;
		this.profileImageUrl = profileImageUrl;
		this.accessTokenEncrypted = accessTokenEncrypted;
		if (refreshTokenEncrypted != null) {
			this.refreshTokenEncrypted = refreshTokenEncrypted;
		}
		this.tokenExpiresAt = tokenExpiresAt;
		this.connectionStatus = ConnectionStatus.CONNECTED;
		this.connectedAt = connectedAt;
		this.disconnectedAt = null;
		this.lastProfileSyncedAt = connectedAt;
	}

	public Long getSpotifyAccountId() {
		return spotifyAccountId;
	}

	public String getSpotifyUserId() {
		return spotifyUserId;
	}

	public String getSpotifyEmail() {
		return spotifyEmail;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getProfileImageUrl() {
		return profileImageUrl;
	}

	public String getAccessTokenEncrypted() {
		return accessTokenEncrypted;
	}

	public String getRefreshTokenEncrypted() {
		return refreshTokenEncrypted;
	}

	public LocalDateTime getTokenExpiresAt() {
		return tokenExpiresAt;
	}

	public ConnectionStatus getConnectionStatus() {
		return connectionStatus;
	}

	public LocalDateTime getConnectedAt() {
		return connectedAt;
	}

	public LocalDateTime getDisconnectedAt() {
		return disconnectedAt;
	}

	public LocalDateTime getLastProfileSyncedAt() {
		return lastProfileSyncedAt;
	}

	public Long getUserId() {
		return userId;
	}

}
