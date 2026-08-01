package com.setpik.server.performance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/** Flyway의 Performance_Type_Map 테이블을 그대로 매핑한다. */
@Entity
@Table(name = "Performance_Type_Map")
@IdClass(PerformanceTypeMapId.class)
public class PerformanceTypeMap {

	@Id
	@Column(name = "performance_id", nullable = false)
	private Long performanceId;

	@Id
	@Column(name = "performance_type_id", nullable = false)
	private Long performanceTypeId;

	protected PerformanceTypeMap() {
	}

	public Long getPerformanceId() {
		return performanceId;
	}

	public Long getPerformanceTypeId() {
		return performanceTypeId;
	}

}
