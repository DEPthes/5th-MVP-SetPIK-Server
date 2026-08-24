package com.setpik.server.performanceview.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public record PerformanceViewResponse(
	Long viewId,
	Long performanceId,
	String performanceName,
	String posterUrl,
	LocalDate startDate,
	String venueName,
	Long analysisId,
	Integer matchedArtistCount,
	OffsetDateTime viewedAt
) {
	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	public static PerformanceViewResponse from(PerformanceViewSummary summary) {
		return new PerformanceViewResponse(
			summary.viewId(),
			summary.performanceId(),
			summary.performanceName(),
			summary.posterUrl(),
			summary.startDate(),
			summary.venueName(),
			summary.analysisId(),
			summary.matchedArtistCount(),
			summary.viewedAt().atZone(KST).toOffsetDateTime()
		);
	}
}
