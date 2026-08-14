package com.setpik.server.performanceview.dto;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public record PerformanceViewCreateResponse(
	Long viewId,
	boolean created,
	OffsetDateTime viewedAt
) {
	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	public static PerformanceViewCreateResponse of(
		Long viewId,
		boolean created,
		LocalDateTime viewedAt
	) {
		return new PerformanceViewCreateResponse(
			viewId,
			created,
			viewedAt.atZone(KST).toOffsetDateTime()
		);
	}
}
