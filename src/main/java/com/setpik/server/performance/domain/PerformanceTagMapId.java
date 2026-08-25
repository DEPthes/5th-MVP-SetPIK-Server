package com.setpik.server.performance.domain;

import java.io.Serializable;
import java.util.Objects;

public class PerformanceTagMapId implements Serializable {

	private Long performanceId;
	private Long performanceTagId;

	public PerformanceTagMapId() {
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) return true;
		if (!(object instanceof PerformanceTagMapId that)) return false;
		return Objects.equals(performanceId, that.performanceId)
			&& Objects.equals(performanceTagId, that.performanceTagId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(performanceId, performanceTagId);
	}
}
