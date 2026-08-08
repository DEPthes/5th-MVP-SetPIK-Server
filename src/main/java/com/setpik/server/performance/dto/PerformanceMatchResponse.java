package com.setpik.server.performance.dto;

import java.time.OffsetDateTime;

public record PerformanceMatchResponse(
	Long analysisId,
	int matchedPerformanceCount,
	OffsetDateTime calculatedAt
) {
}
