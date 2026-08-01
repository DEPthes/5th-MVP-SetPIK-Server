package com.setpik.server.performance.domain;

import java.io.Serializable;
import java.util.Objects;

public class PerformanceGenreId implements Serializable {

	private Long performanceId;
	private Long genreId;

	public PerformanceGenreId() {
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) return true;
		if (!(object instanceof PerformanceGenreId that)) return false;
		return Objects.equals(performanceId, that.performanceId)
			&& Objects.equals(genreId, that.genreId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(performanceId, genreId);
	}
}
