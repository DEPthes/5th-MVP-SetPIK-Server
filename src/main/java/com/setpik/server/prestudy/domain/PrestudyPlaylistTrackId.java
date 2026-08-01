package com.setpik.server.prestudy.domain;

import java.io.Serializable;
import java.util.Objects;

public class PrestudyPlaylistTrackId implements Serializable {

	private Long prestudyPlaylistId;
	private Long trackId;

	public PrestudyPlaylistTrackId() {
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) return true;
		if (!(object instanceof PrestudyPlaylistTrackId that)) return false;
		return Objects.equals(prestudyPlaylistId, that.prestudyPlaylistId)
			&& Objects.equals(trackId, that.trackId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(prestudyPlaylistId, trackId);
	}
}
