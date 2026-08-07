package com.setpik.server.performance.controller;

import com.setpik.server.common.api.ApiResponse;
import com.setpik.server.common.api.PageResponse;
import com.setpik.server.common.config.SwaggerConfig;
import com.setpik.server.performance.dto.MatchedArtistResponse;
import com.setpik.server.performance.dto.PerformanceRecommendationResponse;
import com.setpik.server.performance.service.PerformanceService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/analyses/{analysisId}")
@Tag(name = "공연 추천", description = "분석 기반 공연 매칭 조회 API")
@SecurityRequirement(name = SwaggerConfig.BEARER_AUTH)
public class AnalysisPerformanceController {

	private final PerformanceService performanceService;

	public AnalysisPerformanceController(PerformanceService performanceService) {
		this.performanceService = performanceService;
	}

	@GetMapping("/performances")
	public ApiResponse<PageResponse<PerformanceRecommendationResponse>> getRecommendedPerformances(
		@PathVariable Long analysisId,
		@RequestParam(defaultValue = "0") @Min(0) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
	) {
		return ApiResponse.success(performanceService.getRecommendedPerformances(analysisId, page, size));
	}

	@GetMapping("/performances/{performanceId}/matched-artists")
	public ApiResponse<List<MatchedArtistResponse>> getMatchedArtists(
		@PathVariable Long analysisId,
		@PathVariable Long performanceId
	) {
		return ApiResponse.success(performanceService.getMatchedArtists(analysisId, performanceId));
	}
}