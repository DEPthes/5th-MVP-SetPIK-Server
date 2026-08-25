package com.setpik.server.kopis.dto;

import java.time.LocalDate;
import java.util.List;

public record KopisPerformanceDetail(
	String kopisPerformanceId,
	String performanceName,
	LocalDate startDate,
	LocalDate endDate,
	String posterUrl,
	String bookingUrl,
	String status,
	String priceText,
	String runningTime,
	String ageRestriction,
	String area,
	String genreName,
	String facilityId,
	String venueName,
	List<String> artistNames,
	boolean festival,
	boolean international
) {
	public KopisPerformanceDetail(
		String kopisPerformanceId, String performanceName, LocalDate startDate, LocalDate endDate,
		String posterUrl, String bookingUrl, String status, String priceText,
		String runningTime, String ageRestriction, String area, String genreName,
		String facilityId, String venueName, List<String> artistNames, boolean festival
	) {
		this(kopisPerformanceId, performanceName, startDate, endDate, posterUrl, bookingUrl,
			status, priceText, runningTime, ageRestriction, area, genreName, facilityId,
			venueName, artistNames, festival, false);
	}

	public KopisPerformanceDetail(
		String kopisPerformanceId,
		String performanceName,
		LocalDate startDate,
		LocalDate endDate,
		String posterUrl,
		String bookingUrl,
		String status,
		String priceText,
		String area,
		String genreName,
		String facilityId,
		String venueName,
		List<String> artistNames
	) {
		this(kopisPerformanceId, performanceName, startDate, endDate, posterUrl, bookingUrl,
			status, priceText, null, null, area, genreName, facilityId, venueName,
			artistNames, false, false);
	}
}
