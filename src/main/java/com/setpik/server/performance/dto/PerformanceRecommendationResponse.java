package com.setpik.server.performance.dto;

import com.setpik.server.performance.domain.Performance;
import com.setpik.server.performance.domain.PerformanceMatch;
import com.setpik.server.performance.domain.Venue;
import java.time.LocalDate;
import java.util.List;

public record PerformanceRecommendationResponse(
	Long matchId,
	Long performanceId,
	String performanceName,
	Byte matchPriority,
	Integer matchedArtistCount,
	Byte matchRatio,
	String recommendationReason,
	String posterUrl,
	LocalDate startDate,
	LocalDate endDate,
	String venueName,
	String region,
	List<String> artistNames,
	String performanceType,
	String performanceStatus,
	Integer minTicketPrice
) {
	public static PerformanceRecommendationResponse of(
		PerformanceMatch match,
		Performance performance,
		Venue venue,
		String performanceType,
		List<String> artistNames
	) {
		return new PerformanceRecommendationResponse(
			match.getMatchId(),
			match.getPerformanceId(),
			performance.getPerformanceName(),
			match.getMatchPriority(),
			match.getMatchedArtistCount(),
			match.getMatchRatio(),
			match.getRecommendationReason(),
			performance.getPosterUrl(),
			performance.getStartDate(),
			performance.getEndDate(),
			venue == null ? null : venue.getVenueName(),
			venue == null ? null : venue.getCity(),
			artistNames,
			performanceType,
			performance.getPerformanceStatus().name(),
			performance.getMinTicketPrice()
		);
	}
}
