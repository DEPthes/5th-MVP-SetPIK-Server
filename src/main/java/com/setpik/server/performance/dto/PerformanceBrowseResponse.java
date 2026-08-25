package com.setpik.server.performance.dto;

import com.setpik.server.performance.domain.Performance;
import com.setpik.server.performance.domain.Venue;
import java.time.LocalDate;
import java.util.List;
import com.setpik.server.prestudy.dto.PrestudyPlaylistCardStatus;

public record PerformanceBrowseResponse(
	Long performanceId,
	String performanceName,
	String posterUrl,
	LocalDate startDate,
	LocalDate endDate,
	String venueName,
	String region,
	List<String> artistNames,
	String performanceType,
	String performanceStatus,
	Integer minTicketPrice,
	Integer recommendationScore,
	Long prestudyPlaylistId,
	String creationStatus,
	String spotifyPlaylistId
) {
	public static PerformanceBrowseResponse of(
		Performance performance,
		Venue venue,
		String performanceType,
		List<String> artistNames,
		Integer recommendationScore,
		PrestudyPlaylistCardStatus prestudyStatus
	) {
		return new PerformanceBrowseResponse(
			performance.getPerformanceId(),
			performance.getPerformanceName(),
			performance.getPosterUrl(),
			performance.getStartDate(),
			performance.getEndDate(),
			venue == null ? null : venue.getVenueName(),
			venue == null ? null : venue.getCity(),
			artistNames,
			performanceType,
			performance.getPerformanceStatus().name(),
			performance.getMinTicketPrice(),
			recommendationScore,
			prestudyStatus == null ? null : prestudyStatus.prestudyPlaylistId(),
			prestudyStatus == null ? null : prestudyStatus.creationStatus(),
			prestudyStatus == null ? null : prestudyStatus.spotifyPlaylistId()
		);
	}
}
