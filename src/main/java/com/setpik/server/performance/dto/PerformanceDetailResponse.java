package com.setpik.server.performance.dto;

import com.setpik.server.performance.domain.Performance;
import com.setpik.server.performance.domain.Venue;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PerformanceDetailResponse(
	Long performanceId,
	String performanceName,
	LocalDate startDate,
	LocalDate endDate,
	String posterUrl,
	String bookingUrl,
	String performanceStatus,
	String priceType,
	String ticketPriceText,
	Integer favoriteCount,
	VenueResponse venue
) {
	public static PerformanceDetailResponse of(Performance performance, Venue venue) {
		return new PerformanceDetailResponse(
			performance.getPerformanceId(),
			performance.getPerformanceName(),
			performance.getStartDate(),
			performance.getEndDate(),
			performance.getPosterUrl(),
			performance.getBookingUrl(),
			performance.getPerformanceStatus().name(),
			performance.getPriceType(),
			performance.getTicketPriceText(),
			performance.getFavoriteCount(),
			VenueResponse.from(venue)
		);
	}

	public record VenueResponse(
		Long venueId,
		String venueName,
		String city,
		String district,
		String address,
		BigDecimal latitude,
		BigDecimal longitude
	) {
		public static VenueResponse from(Venue venue) {
			return new VenueResponse(
				venue.getVenueId(),
				venue.getVenueName(),
				venue.getCity(),
				venue.getDistrict(),
				venue.getAddress(),
				venue.getLatitude(),
				venue.getLongitude()
			);
		}
	}
}