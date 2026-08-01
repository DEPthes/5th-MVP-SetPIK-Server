package com.setpik.server.playlist.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** Flyway의 Playlist_tracks 테이블을 그대로 매핑한다. */
@Entity
@Table(name = "Playlist_tracks")
public class PlaylistTrack {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "playlist_track_id", nullable = false)
	private Long playlistTrackId;

	@Column(name = "track_position", nullable = false)
	private Integer trackPosition;

	@Column(name = "added_at", nullable = true)
	private LocalDateTime addedAt;

	@Column(name = "playlist_id", nullable = false)
	private Long playlistId;

	@Column(name = "track_id", nullable = false)
	private Long trackId;

	protected PlaylistTrack() {
	}

	public Long getPlaylistTrackId() {
		return playlistTrackId;
	}

	public Integer getTrackPosition() {
		return trackPosition;
	}

	public LocalDateTime getAddedAt() {
		return addedAt;
	}

	public Long getPlaylistId() {
		return playlistId;
	}

	public Long getTrackId() {
		return trackId;
	}

}
