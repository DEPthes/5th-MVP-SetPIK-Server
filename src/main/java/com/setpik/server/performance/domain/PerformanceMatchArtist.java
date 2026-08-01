package com.setpik.server.performance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/** Flyway의 Performance_Match_Artists 테이블을 그대로 매핑한다. */
@Entity
@Table(name = "Performance_Match_Artists")
@IdClass(PerformanceMatchArtistId.class)
public class PerformanceMatchArtist {

	@Id
	@Column(name = "match_id", nullable = false)
	private Long matchId;

	@Id
	@Column(name = "artist_id", nullable = false)
	private Long artistId;

	@Column(name = "occurrence_count", nullable = false)
	private Integer occurrenceCount;

	protected PerformanceMatchArtist() {
	}

	public Long getMatchId() {
		return matchId;
	}

	public Long getArtistId() {
		return artistId;
	}

	public Integer getOccurrenceCount() {
		return occurrenceCount;
	}

}
