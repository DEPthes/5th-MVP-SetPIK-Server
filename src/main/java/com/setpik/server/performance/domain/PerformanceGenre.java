package com.setpik.server.performance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/** Flyway의 Performance_Genres 테이블을 그대로 매핑한다. */
@Entity
@Table(name = "Performance_Genres")
@IdClass(PerformanceGenreId.class)
public class PerformanceGenre {

	@Id
	@Column(name = "performance_id", nullable = false)
	private Long performanceId;

	@Id
	@Column(name = "genre_id", nullable = false)
	private Long genreId;

	@Column(name = "source_type", nullable = false, length = 50)
	private String sourceType;

	protected PerformanceGenre() {
	}

	public PerformanceGenre(Long performanceId, Long genreId, String sourceType) {
		this.performanceId = performanceId;
		this.genreId = genreId;
		this.sourceType = sourceType;
	}

	public Long getPerformanceId() {
		return performanceId;
	}

	public Long getGenreId() {
		return genreId;
	}

	public String getSourceType() {
		return sourceType;
	}

}
