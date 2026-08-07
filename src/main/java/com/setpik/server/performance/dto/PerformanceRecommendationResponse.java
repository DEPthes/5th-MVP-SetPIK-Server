package com.setpik.server.performance.dto;

import com.setpik.server.performance.domain.PerformanceMatch;

public record PerformanceRecommendationResponse(
	Long matchId,
	Long performanceId,
	String performanceName,
	Byte matchPriority,
	Integer matchedArtistCount,
	Byte matchRatio,
	String recommendationReason
) {
	public static PerformanceRecommendationResponse of(PerformanceMatch match, String performanceName) {
		return new PerformanceRecommendationResponse(
			match.getMatchId(),
			match.getPerformanceId(),
			performanceName,
			match.getMatchPriority(),
			match.getMatchedArtistCount(),
			match.getMatchRatio(),
			match.getRecommendationReason()
		);
	}
}