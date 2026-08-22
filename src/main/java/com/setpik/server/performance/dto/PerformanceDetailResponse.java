package com.setpik.server.performance.dto;

import com.setpik.server.performance.domain.Performance;
import com.setpik.server.performance.domain.PerformanceArtist;
import com.setpik.server.performance.domain.Venue;
import com.setpik.server.artist.domain.Artist;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record PerformanceDetailResponse(
	Long performanceId,
	String performanceName,
	LocalDate startDate,
	LocalDate endDate,
	String posterUrl,
	String bookingUrl,
	String ticketPriceText,
	String runningTime,
	String ageRestriction,
	String performanceStatus,
	VenueResponse venue,
	List<ArtistResponse> artists
) {
	public PerformanceDetailResponse(
		Long performanceId,
		String performanceName,
		LocalDate startDate,
		LocalDate endDate,
		String posterUrl,
		String bookingUrl,
		String performanceStatus,
		VenueResponse venue
	) {
		this(performanceId, performanceName, startDate, endDate, posterUrl, bookingUrl,
			null, null, null, performanceStatus, venue, List.of());
	}

	public static PerformanceDetailResponse of(
		Performance performance,
		Venue venue,
		List<PerformanceArtist> lineup,
		Map<Long, Artist> artistById
	) {
		return new PerformanceDetailResponse(
			performance.getPerformanceId(),
			performance.getPerformanceName(),
			performance.getStartDate(),
			performance.getEndDate(),
			performance.getPosterUrl(),
			performance.getBookingUrl(),
			performance.getTicketPriceText(),
			performance.getRunningTime(),
			performance.getAgeRestriction(),
			performance.getPerformanceStatus().name(),
			VenueResponse.from(venue),
			lineup.stream()
				.filter(mapping -> artistById.containsKey(mapping.getArtistId()))
				.map(mapping -> ArtistResponse.of(mapping, artistById.get(mapping.getArtistId())))
				.toList()
		);
	}

	public record ArtistResponse(
		Long artistId,
		String artistName,
		Boolean isHeadliner,
		Long lineupOrder
	) {
		public static ArtistResponse of(PerformanceArtist mapping, Artist artist) {
			return new ArtistResponse(
				artist.getArtistId(),
				artist.getArtistName(),
				mapping.getIsHeadliner(),
				mapping.getLineupOrder()
			);
		}
	}

	public record VenueResponse(
		Long venueId,
		String venueName,
		String city
	) {
		public static VenueResponse from(Venue venue) {
			return new VenueResponse(
				venue.getVenueId(),
				venue.getVenueName(),
				venue.getCity()
			);
		}
	}
}
