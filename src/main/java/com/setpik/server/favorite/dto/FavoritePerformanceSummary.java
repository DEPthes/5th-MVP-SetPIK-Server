package com.setpik.server.favorite.dto;

import com.setpik.server.performance.domain.PerformanceStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 관심 공연 목록 조회에 필요한 DB 조인 결과. */
public record FavoritePerformanceSummary(
	Long favoriteId,
	Long performanceId,
	String performanceName,
	String posterUrl,
	LocalDate startDate,
	String venueName,
	PerformanceStatus performanceStatus,
	String priceType,
	String ticketPriceText,
	LocalDateTime savedAt
) {
}
