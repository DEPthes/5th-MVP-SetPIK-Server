package com.setpik.server.artist.domain;

import com.setpik.server.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** KOPIS 원문 출연진과 검증된 Spotify 아티스트 ID의 관계 및 조회 결과를 보관한다. */
@Entity
@Table(name = "Artist_Aliases")
public class ArtistAlias extends BaseEntity {

	@Id
	@Column(name = "kopis_artist_id", nullable = false)
	private Long kopisArtistId;

	@Column(name = "spotify_artist_id", length = 255)
	private String spotifyArtistId;

	@Column(name = "source_type", length = 50)
	private String sourceType;

	@Column(name = "external_entity_id", length = 255)
	private String externalEntityId;

	@Enumerated(EnumType.STRING)
	@Column(name = "resolution_status", nullable = false, length = 50)
	private ArtistAliasResolutionStatus resolutionStatus;

	@Column(name = "last_attempted_at", nullable = false)
	private LocalDateTime lastAttemptedAt;

	protected ArtistAlias() {
	}

	private ArtistAlias(Long kopisArtistId, String spotifyArtistId, String sourceType,
		String externalEntityId, ArtistAliasResolutionStatus resolutionStatus, LocalDateTime attemptedAt) {
		this.kopisArtistId = kopisArtistId;
		this.spotifyArtistId = spotifyArtistId;
		this.sourceType = sourceType;
		this.externalEntityId = externalEntityId;
		this.resolutionStatus = resolutionStatus;
		this.lastAttemptedAt = attemptedAt;
	}

	public static ArtistAlias resolved(Long kopisArtistId, String spotifyArtistId,
		String sourceType, String externalEntityId, LocalDateTime attemptedAt) {
		return new ArtistAlias(kopisArtistId, spotifyArtistId, sourceType, externalEntityId,
			ArtistAliasResolutionStatus.RESOLVED, attemptedAt);
	}

	public static ArtistAlias unresolved(Long kopisArtistId, ArtistAliasResolutionStatus status,
		LocalDateTime attemptedAt) {
		return new ArtistAlias(kopisArtistId, null, null, null, status, attemptedAt);
	}

	public Long getKopisArtistId() {
		return kopisArtistId;
	}

	public String getSpotifyArtistId() {
		return spotifyArtistId;
	}

	public ArtistAliasResolutionStatus getResolutionStatus() {
		return resolutionStatus;
	}
}
