package com.setpik.server.performanceview.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PerformanceViewCreateRequest(
	@NotNull(message = "performanceId는 필수입니다.")
	@Positive(message = "performanceId는 양수여야 합니다.")
	Long performanceId,

	@NotNull(message = "analysisId는 필수입니다.")
	@Positive(message = "analysisId는 양수여야 합니다.")
	Long analysisId
) {
}
