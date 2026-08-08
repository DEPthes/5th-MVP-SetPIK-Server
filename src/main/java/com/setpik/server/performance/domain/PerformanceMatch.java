package com.setpik.server.performance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** Flyway의 Performance_Matches 테이블을 그대로 매핑한다. */
@Entity
@Table(name = "Performance_Matches")
public class PerformanceMatch {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "match_id", nullable = false)
	private Long matchId;

	@Column(name = "match_priority", nullable = false)
	private Byte matchPriority;

	@Column(name = "matched_artist_count", nullable = false)
	private Integer matchedArtistCount;

	@Column(name = "lineup_artist_count", nullable = false)
	private Integer lineupArtistCount;

	@Column(name = "match_ratio", nullable = true)
	private Byte matchRatio;

	@Column(name = "recommendation_reason", nullable = false, length = 500)
	private String recommendationReason;

	@Column(name = "calculated_at", nullable = false)
	private LocalDateTime calculatedAt;

	@Column(name = "performance_id", nullable = false)
	private Long performanceId;

	@Column(name = "analysis_id", nullable = false)
	private Long analysisId;

	@Column(name = "genre_id", nullable = true)
	private Long genreId;

	protected PerformanceMatch() {
	}

	public static PerformanceMatch create(
		Byte matchPriority,
		Integer matchedArtistCount,
		Integer lineupArtistCount,
		Byte matchRatio,
		String recommendationReason,
		LocalDateTime calculatedAt,
		Long performanceId,
		Long analysisId,
		Long genreId
	) {
		PerformanceMatch match = new PerformanceMatch();
		match.matchPriority = matchPriority;
		match.matchedArtistCount = matchedArtistCount;
		match.lineupArtistCount = lineupArtistCount;
		match.matchRatio = matchRatio;
		match.recommendationReason = recommendationReason;
		match.calculatedAt = calculatedAt;
		match.performanceId = performanceId;
		match.analysisId = analysisId;
		match.genreId = genreId;
		return match;
	}

	public Long getMatchId() {
		return matchId;
	}

	public Byte getMatchPriority() {
		return matchPriority;
	}

	public Integer getMatchedArtistCount() {
		return matchedArtistCount;
	}

	public Integer getLineupArtistCount() {
		return lineupArtistCount;
	}

	public Byte getMatchRatio() {
		return matchRatio;
	}

	public String getRecommendationReason() {
		return recommendationReason;
	}

	public LocalDateTime getCalculatedAt() {
		return calculatedAt;
	}

	public Long getPerformanceId() {
		return performanceId;
	}

	public Long getAnalysisId() {
		return analysisId;
	}

	public Long getGenreId() {
		return genreId;
	}

}
