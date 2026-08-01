package com.setpik.server.playlist.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/** Flyway의 Track_Artists 테이블을 그대로 매핑한다. */
@Entity
@Table(name = "Track_Artists")
@IdClass(TrackArtistId.class)
public class TrackArtist {

	@Id
	@Column(name = "track_id", nullable = false)
	private Long trackId;

	@Id
	@Column(name = "artist_id", nullable = false)
	private Long artistId;

	@Column(name = "artist_order", nullable = false)
	private Short artistOrder;

	protected TrackArtist() {
	}

	public Long getTrackId() {
		return trackId;
	}

	public Long getArtistId() {
		return artistId;
	}

	public Short getArtistOrder() {
		return artistOrder;
	}

}
