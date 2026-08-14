package com.setpik.server.prestudy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/** Flyway의 Prestudy_Playlist_Tracks 테이블을 그대로 매핑한다. */
@Entity
@Table(name = "Prestudy_Playlist_Tracks")
@IdClass(PrestudyPlaylistTrackId.class)
public class PrestudyPlaylistTrack {

	@Id
	@Column(name = "prestudy_playlist_id", nullable = false)
	private Long prestudyPlaylistId;

	@Id
	@Column(name = "track_id", nullable = false)
	private Long trackId;

	@Column(name = "track_order", nullable = false)
	private Integer trackOrder;

	@Enumerated(EnumType.STRING)
	@Column(name = "source_type", nullable = false, length = 50)
	private SourceType sourceType;

	@Column(name = "is_new_artist_track", nullable = false)
	private Boolean isNewArtistTrack;

	protected PrestudyPlaylistTrack() {
	}

	public PrestudyPlaylistTrack(
		Long prestudyPlaylistId,
		Long trackId,
		Integer trackOrder,
		SourceType sourceType,
		Boolean isNewArtistTrack
	) {
		this.prestudyPlaylistId = prestudyPlaylistId;
		this.trackId = trackId;
		this.trackOrder = trackOrder;
		this.sourceType = sourceType;
		this.isNewArtistTrack = isNewArtistTrack;
	}

	public Long getPrestudyPlaylistId() {
		return prestudyPlaylistId;
	}

	public Long getTrackId() {
		return trackId;
	}

	public Integer getTrackOrder() {
		return trackOrder;
	}

	public SourceType getSourceType() {
		return sourceType;
	}

	public Boolean getIsNewArtistTrack() {
		return isNewArtistTrack;
	}

}