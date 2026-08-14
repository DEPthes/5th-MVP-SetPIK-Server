package com.setpik.server.favorite.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public record FavoritePerformanceResponse(
	Long favoriteId,
	Long performanceId,
	String performanceName,
	String posterUrl,
	LocalDate startDate,
	String venueName,
	OffsetDateTime savedAt
) {
	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	public static FavoritePerformanceResponse from(FavoritePerformanceSummary summary) {
		return new FavoritePerformanceResponse(
			summary.favoriteId(),
			summary.performanceId(),
			summary.performanceName(),
			summary.posterUrl(),
			summary.startDate(),
			summary.venueName(),
			summary.savedAt().atZone(KST).toOffsetDateTime()
		);
	}
}
