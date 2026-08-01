package com.setpik.server.playlist.domain;

import com.setpik.server.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Flyway의 Tracks 테이블을 그대로 매핑한다. */
@Entity
@Table(name = "Tracks")
public class Track extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "track_id", nullable = false)
	private Long trackId;

	@Column(name = "spotify_track_id", nullable = false, length = 255)
	private String spotifyTrackId;

	@Column(name = "track_name", nullable = false, length = 255)
	private String trackName;

	@Column(name = "album_name", nullable = true, length = 255)
	private String albumName;

	@Column(name = "album_image_url", nullable = true, length = 2048)
	private String albumImageUrl;

	@Column(name = "spotify_track_url", nullable = true, length = 2048)
	private String spotifyTrackUrl;

	@Column(name = "preview_url", nullable = true, length = 2048)
	private String previewUrl;

	@Column(name = "duration_ms", nullable = true)
	private Integer durationMs;

	@Column(name = "is_playable", nullable = false)
	private Boolean isPlayable;

	protected Track() {
	}

	public Long getTrackId() {
		return trackId;
	}

	public String getSpotifyTrackId() {
		return spotifyTrackId;
	}

	public String getTrackName() {
		return trackName;
	}

	public String getAlbumName() {
		return albumName;
	}

	public String getAlbumImageUrl() {
		return albumImageUrl;
	}

	public String getSpotifyTrackUrl() {
		return spotifyTrackUrl;
	}

	public String getPreviewUrl() {
		return previewUrl;
	}

	public Integer getDurationMs() {
		return durationMs;
	}

	public Boolean getIsPlayable() {
		return isPlayable;
	}

}
