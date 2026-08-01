package com.setpik.server.performance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/** Flyway의 Performance_Artists 테이블을 그대로 매핑한다. */
@Entity
@Table(name = "Performance_Artists")
@IdClass(PerformanceArtistId.class)
public class PerformanceArtist {

	@Id
	@Column(name = "artist_id", nullable = false)
	private Long artistId;

	@Id
	@Column(name = "performance_id", nullable = false)
	private Long performanceId;

	@Column(name = "lineup_order", nullable = true)
	private Long lineupOrder;

	@Column(name = "is_headliner", nullable = false)
	private Boolean isHeadliner;

	protected PerformanceArtist() {
	}

	public Long getArtistId() {
		return artistId;
	}

	public Long getPerformanceId() {
		return performanceId;
	}

	public Long getLineupOrder() {
		return lineupOrder;
	}

	public Boolean getIsHeadliner() {
		return isHeadliner;
	}

}
