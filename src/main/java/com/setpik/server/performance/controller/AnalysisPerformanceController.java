package com.setpik.server.performance.controller;

import com.setpik.server.common.api.ApiResponse;
import com.setpik.server.common.api.PageResponse;
import com.setpik.server.common.config.SwaggerConfig;
import com.setpik.server.performance.dto.MatchedArtistResponse;
import com.setpik.server.performance.dto.PerformanceMatchRequest;
import com.setpik.server.performance.dto.PerformanceMatchResponse;
import com.setpik.server.performance.dto.PerformanceRecommendationResponse;
import com.setpik.server.performance.service.PerformanceMatchingService;
import com.setpik.server.performance.service.PerformanceService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import com.setpik.server.common.exception.BusinessException;
import com.setpik.server.common.exception.ErrorCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/analyses/{analysisId}")
@Tag(name = "공연 추천", description = "분석 기반 공연 매칭 조회 API")
@SecurityRequirement(name = SwaggerConfig.BEARER_AUTH)
public class AnalysisPerformanceController {

	private final PerformanceService performanceService;
	private final PerformanceMatchingService performanceMatchingService;

	public AnalysisPerformanceController(
		PerformanceService performanceService,
		PerformanceMatchingService performanceMatchingService
	) {
		this.performanceService = performanceService;
		this.performanceMatchingService = performanceMatchingService;
	}

	@PostMapping("/matches")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<PerformanceMatchResponse> calculateMatches(
		@PathVariable @Positive Long analysisId,
		@RequestBody(required = false) @Valid PerformanceMatchRequest request,
		@AuthenticationPrincipal Jwt jwt
	) {
		PerformanceMatchRequest criteria = request == null ? new PerformanceMatchRequest(null, null) : request;
		return ApiResponse.created(
			"공연 매칭 계산이 완료되었습니다.",
			performanceMatchingService.calculate(userId(jwt), analysisId, criteria)
		);
	}

	@GetMapping("/performances")
	public ApiResponse<PageResponse<PerformanceRecommendationResponse>> getRecommendedPerformances(
		@PathVariable @Positive Long analysisId,
		@RequestParam(defaultValue = "0") @Min(0) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
		@RequestParam(defaultValue = "matchPriority,asc") String sort,
		@AuthenticationPrincipal Jwt jwt
	) {
		return ApiResponse.success(performanceService.getRecommendedPerformances(
			userId(jwt), analysisId, page, size, sort));
	}

	@GetMapping("/performances/{performanceId}/matched-artists")
	public ApiResponse<List<MatchedArtistResponse>> getMatchedArtists(
		@PathVariable @Positive Long analysisId,
		@PathVariable @Positive Long performanceId,
		@AuthenticationPrincipal Jwt jwt
	) {
		return ApiResponse.success(performanceService.getMatchedArtists(
			userId(jwt), analysisId, performanceId));
	}

	private Long userId(Jwt jwt) {
		try {
			return Long.valueOf(jwt.getSubject());
		} catch (NumberFormatException exception) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}
	}
}
