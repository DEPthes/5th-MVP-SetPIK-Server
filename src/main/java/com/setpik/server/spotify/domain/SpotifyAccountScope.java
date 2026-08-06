package com.setpik.server.spotify.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** Flyway의 Spotify_account_scopes 테이블을 그대로 매핑한다. */
@Entity
@Table(name = "Spotify_account_scopes")
@IdClass(SpotifyAccountScopeId.class)
public class SpotifyAccountScope {

	@Id
	@Column(name = "scope_name", nullable = false, length = 255)
	private String scopeName;

	@Id
	@Column(name = "spotify_account_id", nullable = false)
	private Long spotifyAccountId;

	@Column(name = "is_granted", nullable = false)
	private Boolean isGranted;

	@Column(name = "granted_at", nullable = false)
	private LocalDateTime grantedAt;

	@Column(name = "revoked_at", nullable = true)
	private LocalDateTime revokedAt;

	protected SpotifyAccountScope() {
	}

	/** Spotify가 실제로 승인해 준 scope를 계정에 연결한다. */
	public static SpotifyAccountScope grant(
		String scopeName,
		Long spotifyAccountId,
		LocalDateTime grantedAt
	) {
		SpotifyAccountScope scope = new SpotifyAccountScope();
		scope.scopeName = scopeName;
		scope.spotifyAccountId = spotifyAccountId;
		scope.isGranted = true;
		scope.grantedAt = grantedAt;
		return scope;
	}

	public String getScopeName() {
		return scopeName;
	}

	public Long getSpotifyAccountId() {
		return spotifyAccountId;
	}

	public Boolean getIsGranted() {
		return isGranted;
	}

	public LocalDateTime getGrantedAt() {
		return grantedAt;
	}

	public LocalDateTime getRevokedAt() {
		return revokedAt;
	}

}
