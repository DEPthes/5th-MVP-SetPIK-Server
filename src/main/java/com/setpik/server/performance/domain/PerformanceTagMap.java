package com.setpik.server.performance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/** Flyway의 Performance_Tag_Map 테이블을 그대로 매핑한다. */
@Entity
@Table(name = "Performance_Tag_Map")
@IdClass(PerformanceTagMapId.class)
public class PerformanceTagMap {

	@Id
	@Column(name = "performance_id", nullable = false)
	private Long performanceId;

	@Id
	@Column(name = "performance_tag_id", nullable = false)
	private Long performanceTagId;

	protected PerformanceTagMap() {
	}

	public PerformanceTagMap(Long performanceId, Long performanceTagId) {
		this.performanceId = performanceId;
		this.performanceTagId = performanceTagId;
	}

	public Long getPerformanceId() {
		return performanceId;
	}

	public Long getPerformanceTagId() {
		return performanceTagId;
	}

}
