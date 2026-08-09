package com.setpik.server.analysis.domain;

import com.setpik.server.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/** Flyway의 Analysis_Artists 테이블을 그대로 매핑한다. */
@Entity
@Table(name = "Analysis_Artists")
@IdClass(AnalysisArtistId.class)
public class AnalysisArtist extends BaseEntity {

	@Id
	@Column(name = "artist_id", nullable = false)
	private Long artistId;

	@Id
	@Column(name = "analysis_id", nullable = false)
	private Long analysisId;

	@Column(name = "occurrence_count", nullable = false)
	private Integer occurrenceCount;

	@Column(name = "popularity_snapshot", nullable = true)
	private Short popularitySnapshot;

	@Column(name = "is_major", nullable = false)
	private Boolean isMajor;

	@Column(name = "is_excluded", nullable = false)
	private Boolean isExcluded;

	@Column(name = "display_rank", nullable = true)
	private Integer displayRank;

	@Column(name = "origin", nullable = false, length = 50)
	private String origin;

	protected AnalysisArtist() {
	}

	public AnalysisArtist(Long analysisId, Long artistId, Integer occurrenceCount,
						  Short popularitySnapshot, Boolean isMajor, Integer displayRank) {
		this.analysisId = analysisId;
		this.artistId = artistId;
		this.occurrenceCount = occurrenceCount;
		this.popularitySnapshot = popularitySnapshot;
		this.isMajor = isMajor;
		this.isExcluded = false;
		this.displayRank = displayRank;
		this.origin = "SPOTIFY";
	}

	public void changeExcluded(Boolean isExcluded) {
		this.isExcluded = isExcluded;
	}

	public Long getArtistId() {
		return artistId;
	}

	public Long getAnalysisId() {
		return analysisId;
	}

	public Integer getOccurrenceCount() {
		return occurrenceCount;
	}

	public Short getPopularitySnapshot() {
		return popularitySnapshot;
	}

	public Boolean getIsMajor() {
		return isMajor;
	}

	public Boolean getIsExcluded() {
		return isExcluded;
	}

	public Integer getDisplayRank() {
		return displayRank;
	}

	public String getOrigin() {
		return origin;
	}

}
