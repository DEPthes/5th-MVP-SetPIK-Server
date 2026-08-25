package com.setpik.server.favorite.dto;

import com.setpik.server.performance.service.TicketPriceParser;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

public record FavoritePerformanceResponse(
	Long favoriteId,
	Long performanceId,
	String performanceName,
	String posterUrl,
	LocalDate startDate,
	String venueName,
	String performanceType,
	String performanceStatus,
	List<String> artistNames,
	Integer matchedArtistCount,
	Integer minTicketPrice,
	OffsetDateTime savedAt
) {
	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	public static FavoritePerformanceResponse from(
		FavoritePerformanceSummary summary,
		String performanceType,
		List<String> artistNames,
		Integer matchedArtistCount
	) {
		return new FavoritePerformanceResponse(
			summary.favoriteId(),
			summary.performanceId(),
			summary.performanceName(),
			summary.posterUrl(),
			summary.startDate(),
			summary.venueName(),
			performanceType,
			summary.performanceStatus().name(),
			artistNames,
			matchedArtistCount,
			TicketPriceParser.parseMinPrice(summary.priceType(), summary.ticketPriceText()),
			summary.savedAt().atZone(KST).toOffsetDateTime()
		);
	}
}
