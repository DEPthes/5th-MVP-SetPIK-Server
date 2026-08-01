package com.setpik.server.playlist.domain;

import java.io.Serializable;
import java.util.Objects;

public class TrackArtistId implements Serializable {

	private Long trackId;
	private Long artistId;

	public TrackArtistId() {
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) return true;
		if (!(object instanceof TrackArtistId that)) return false;
		return Objects.equals(trackId, that.trackId)
			&& Objects.equals(artistId, that.artistId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(trackId, artistId);
	}
}
