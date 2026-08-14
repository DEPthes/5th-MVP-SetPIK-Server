package com.setpik.server.favorite.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record FavoritePerformanceCreateRequest(
	@NotNull(message = "performanceId는 필수입니다.")
	@Positive(message = "performanceId는 양수여야 합니다.")
	Long performanceId
) {
}
