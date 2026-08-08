package com.setpik.server.performance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

public record PerformanceMatchRequest(
	@Schema(example = "2026-08-01") LocalDate fromDate,
	@Schema(example = "2026-12-31") LocalDate toDate
) {
}
