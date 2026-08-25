package com.setpik.server.performance.dto;

import com.setpik.server.performance.domain.Performance;
import com.setpik.server.performance.domain.PerformanceMatch;
import com.setpik.server.performance.domain.Venue;
import java.time.LocalDate;
import java.util.List;
import com.setpik.server.prestudy.dto.PrestudyPlaylistCardStatus;

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
	String genreName,
	String performanceType,
	List<String> tags,
	String performanceStatus,
	Integer minTicketPrice,
	Long prestudyPlaylistId,
	String creationStatus,
	String spotifyPlaylistId
) {
	public static PerformanceRecommendationResponse of(
		PerformanceMatch match,
		Performance performance,
		Venue venue,
		String performanceType,
		String genreName,
		List<String> tags,
		List<String> artistNames,
		PrestudyPlaylistCardStatus prestudyStatus
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
			genreName,
			performanceType,
			tags,
			performance.getPerformanceStatus().name(),
			performance.getMinTicketPrice(),
			prestudyStatus == null ? null : prestudyStatus.prestudyPlaylistId(),
			prestudyStatus == null ? null : prestudyStatus.creationStatus(),
			prestudyStatus == null ? null : prestudyStatus.spotifyPlaylistId()
		);
	}
}
