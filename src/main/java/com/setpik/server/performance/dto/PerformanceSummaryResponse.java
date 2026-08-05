package com.setpik.server.performance.dto;

import com.setpik.server.performance.domain.Performance;
import com.setpik.server.performance.domain.Venue;
import java.time.LocalDate;

public record PerformanceSummaryResponse(
	Long performanceId,
	String performanceName,
	String venueName,
	String city,
	LocalDate startDate,
	LocalDate endDate,
	String posterUrl,
	String performanceStatus,
	String priceType,
	String ticketPriceText,
	Integer favoriteCount
) {
	public static PerformanceSummaryResponse of(Performance performance, Venue venue) {
		return new PerformanceSummaryResponse(
			performance.getPerformanceId(),
			performance.getPerformanceName(),
			venue.getVenueName(),
			venue.getCity(),
			performance.getStartDate(),
			performance.getEndDate(),
			performance.getPosterUrl(),
			performance.getPerformanceStatus().name(),
			performance.getPriceType(),
			performance.getTicketPriceText(),
			performance.getFavoriteCount()
		);
	}
}