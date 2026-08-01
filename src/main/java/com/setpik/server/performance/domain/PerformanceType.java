package com.setpik.server.performance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Flyway의 Performance_Types 테이블을 그대로 매핑한다. */
@Entity
@Table(name = "Performance_Types")
public class PerformanceType {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "performance_type_id", nullable = false)
	private Long performanceTypeId;

	@Column(name = "type_code", nullable = false, length = 50)
	private String typeCode;

	@Column(name = "type_name", nullable = false, length = 255)
	private String typeName;

	protected PerformanceType() {
	}

	public Long getPerformanceTypeId() {
		return performanceTypeId;
	}

	public String getTypeCode() {
		return typeCode;
	}

	public String getTypeName() {
		return typeName;
	}

}
