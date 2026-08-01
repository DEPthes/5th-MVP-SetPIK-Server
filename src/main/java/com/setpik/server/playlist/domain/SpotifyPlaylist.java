package com.setpik.server.playlist.domain;

import com.setpik.server.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** Flyway의 Spotify_Playlists 테이블을 그대로 매핑한다. */
@Entity
@Table(name = "Spotify_Playlists")
public class SpotifyPlaylist extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "playlist_id", nullable = false)
	private Long playlistId;

	@Column(name = "spotify_playlist_id", nullable = false, length = 255)
	private String spotifyPlaylistId;

	@Column(name = "playlist_name", nullable = false, length = 255)
	private String playlistName;

	@Column(name = "description", nullable = true, columnDefinition = "TEXT")
	private String description;

	@Column(name = "cover_image_url", nullable = true, length = 2048)
	private String coverImageUrl;

	@Column(name = "is_public", nullable = true)
	private Boolean isPublic;

	@Column(name = "owner_spotify_user_id", nullable = true, length = 255)
	private String ownerSpotifyUserId;

	@Column(name = "snapshot_id", nullable = true, length = 255)
	private String snapshotId;

	@Column(name = "track_count", nullable = false)
	private Integer trackCount;

	@Column(name = "last_synced_at", nullable = true)
	private LocalDateTime lastSyncedAt;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "deleted_at", nullable = true)
	private LocalDateTime deletedAt;

	protected SpotifyPlaylist() {
	}

	public Long getPlaylistId() {
		return playlistId;
	}

	public String getSpotifyPlaylistId() {
		return spotifyPlaylistId;
	}

	public String getPlaylistName() {
		return playlistName;
	}

	public String getDescription() {
		return description;
	}

	public String getCoverImageUrl() {
		return coverImageUrl;
	}

	public Boolean getIsPublic() {
		return isPublic;
	}

	public String getOwnerSpotifyUserId() {
		return ownerSpotifyUserId;
	}

	public String getSnapshotId() {
		return snapshotId;
	}

	public Integer getTrackCount() {
		return trackCount;
	}

	public LocalDateTime getLastSyncedAt() {
		return lastSyncedAt;
	}

	public Long getUserId() {
		return userId;
	}

	public LocalDateTime getDeletedAt() {
		return deletedAt;
	}

}
