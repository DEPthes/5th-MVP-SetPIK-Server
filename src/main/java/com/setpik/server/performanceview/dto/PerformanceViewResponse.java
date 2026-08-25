package com.setpik.server.performanceview.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

public record PerformanceViewResponse(
	Long viewId,
	Long performanceId,
	String performanceName,
	String posterUrl,
	LocalDate startDate,
	String venueName,
	String genreName,
	String performanceType,
	List<String> tags,
	String performanceStatus,
	List<String> artistNames,
	Long analysisId,
	Integer matchedArtistCount,
	OffsetDateTime viewedAt
) {
	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	public static PerformanceViewResponse from(
		PerformanceViewSummary summary,
		String performanceType,
		String genreName,
		List<String> tags,
		List<String> artistNames
	) {
		return new PerformanceViewResponse(
			summary.viewId(),
			summary.performanceId(),
			summary.performanceName(),
			summary.posterUrl(),
			summary.startDate(),
			summary.venueName(),
			genreName,
			performanceType,
			tags,
			summary.performanceStatus().name(),
			artistNames,
			summary.analysisId(),
			summary.matchedArtistCount(),
			summary.viewedAt().atZone(KST).toOffsetDateTime()
		);
	}
}
