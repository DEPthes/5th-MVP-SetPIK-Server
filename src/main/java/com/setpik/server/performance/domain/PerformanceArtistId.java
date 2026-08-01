package com.setpik.server.performance.domain;

import java.io.Serializable;
import java.util.Objects;

public class PerformanceArtistId implements Serializable {

	private Long artistId;
	private Long performanceId;

	public PerformanceArtistId() {
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) return true;
		if (!(object instanceof PerformanceArtistId that)) return false;
		return Objects.equals(artistId, that.artistId)
			&& Objects.equals(performanceId, that.performanceId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(artistId, performanceId);
	}
}
