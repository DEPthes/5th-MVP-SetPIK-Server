package com.setpik.server.performance.domain;

import java.io.Serializable;
import java.util.Objects;

public class PerformanceMatchArtistId implements Serializable {

	private Long matchId;
	private Long artistId;

	public PerformanceMatchArtistId() {
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) return true;
		if (!(object instanceof PerformanceMatchArtistId that)) return false;
		return Objects.equals(matchId, that.matchId)
			&& Objects.equals(artistId, that.artistId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(matchId, artistId);
	}
}
