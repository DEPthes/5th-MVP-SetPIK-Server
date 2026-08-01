package com.setpik.server.performance.domain;

import java.io.Serializable;
import java.util.Objects;

public class PerformanceTypeMapId implements Serializable {

	private Long performanceId;
	private Long performanceTypeId;

	public PerformanceTypeMapId() {
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) return true;
		if (!(object instanceof PerformanceTypeMapId that)) return false;
		return Objects.equals(performanceId, that.performanceId)
			&& Objects.equals(performanceTypeId, that.performanceTypeId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(performanceId, performanceTypeId);
	}
}
