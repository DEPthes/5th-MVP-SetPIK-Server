package com.setpik.server.performance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Flyway의 Performance_Tags 테이블을 그대로 매핑한다. performanceType과 독립적으로 겹칠 수 있는 부가 분류(예: 내한 공연)를 담는다. */
@Entity
@Table(name = "Performance_Tags")
public class PerformanceTag {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "performance_tag_id", nullable = false)
	private Long performanceTagId;

	@Column(name = "tag_code", nullable = false, length = 50)
	private String tagCode;

	@Column(name = "tag_name", nullable = false, length = 255)
	private String tagName;

	protected PerformanceTag() {
	}

	public PerformanceTag(String tagCode, String tagName) {
		this.tagCode = tagCode;
		this.tagName = tagName;
	}

	public Long getPerformanceTagId() {
		return performanceTagId;
	}

	public String getTagCode() {
		return tagCode;
	}

	public String getTagName() {
		return tagName;
	}

}
