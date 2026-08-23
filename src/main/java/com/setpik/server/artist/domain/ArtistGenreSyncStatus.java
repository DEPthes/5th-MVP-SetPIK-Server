package com.setpik.server.artist.domain;

import com.setpik.server.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "Artist_Genre_Sync_Status")
public class ArtistGenreSyncStatus extends BaseEntity {

	@Id
	@Column(name = "artist_id", nullable = false)
	private Long artistId;

	@Enumerated(EnumType.STRING)
	@Column(name = "resolution_status", nullable = false, length = 50)
	private ArtistAliasResolutionStatus resolutionStatus;

	@Column(name = "external_entity_id", length = 255)
	private String externalEntityId;

	@Column(name = "last_attempted_at", nullable = false)
	private LocalDateTime lastAttemptedAt;

	protected ArtistGenreSyncStatus() {
	}

	private ArtistGenreSyncStatus(Long artistId, ArtistAliasResolutionStatus resolutionStatus,
		String externalEntityId, LocalDateTime lastAttemptedAt) {
		this.artistId = artistId;
		this.resolutionStatus = resolutionStatus;
		this.externalEntityId = externalEntityId;
		this.lastAttemptedAt = lastAttemptedAt;
	}

	public static ArtistGenreSyncStatus of(Long artistId, ArtistAliasResolutionStatus status,
		String externalEntityId, LocalDateTime attemptedAt) {
		return new ArtistGenreSyncStatus(artistId, status, externalEntityId, attemptedAt);
	}

	public Long getArtistId() { return artistId; }
	public ArtistAliasResolutionStatus getResolutionStatus() { return resolutionStatus; }
}
