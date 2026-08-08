package com.setpik.server.kopis.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record KopisSyncResponse(
	LocalDate fromDate,
	LocalDate toDate,
	int fetchedPerformanceCount,
	int createdPerformanceCount,
	int updatedPerformanceCount,
	OffsetDateTime syncedAt
) {
}
