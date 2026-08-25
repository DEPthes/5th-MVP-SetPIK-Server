package com.setpik.server.performance.controller;

import com.setpik.server.common.api.ApiResponse;
import com.setpik.server.common.api.PageResponse;
import com.setpik.server.common.config.SwaggerConfig;
import com.setpik.server.common.exception.BusinessException;
import com.setpik.server.common.exception.ErrorCode;
import com.setpik.server.performance.dto.PerformanceBrowseResponse;
import com.setpik.server.performance.dto.PerformanceDetailResponse;
import com.setpik.server.performance.dto.TicketScheduleResponse;
import com.setpik.server.performance.service.PerformanceService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/v1/performances")
@Tag(name = "공연", description = "공연 조회 API")
public class PerformanceController {

	private final PerformanceService performanceService;

	public PerformanceController(PerformanceService performanceService) {
		this.performanceService = performanceService;
	}

	@GetMapping
	@SecurityRequirement(name = SwaggerConfig.BEARER_AUTH)
	public ApiResponse<PageResponse<PerformanceBrowseResponse>> getPerformances(
		@RequestParam(defaultValue = "0") @Min(0) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
		@RequestParam(required = false) String keyword,
		@RequestParam(required = false) String performanceType,
		@RequestParam(required = false) String region,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
		@RequestParam(defaultValue = "recommended,desc") String sort,
		@AuthenticationPrincipal Jwt jwt
	) {
		return ApiResponse.success(performanceService.browsePerformances(
			userId(jwt), keyword, performanceType, region, fromDate, toDate, page, size, sort));
	}

	@GetMapping("/{performanceId}")
	public ApiResponse<PerformanceDetailResponse> getPerformance(
		@PathVariable @Positive Long performanceId
	) {
		return ApiResponse.success(performanceService.getPerformance(performanceId));
	}

	@GetMapping("/{performanceId}/ticket-schedules")
	public ApiResponse<List<TicketScheduleResponse>> getTicketSchedules(
		@PathVariable @Positive Long performanceId
	) {
		return ApiResponse.success(performanceService.getTicketSchedules(performanceId));
	}

	private Long userId(Jwt jwt) {
		try {
			return Long.valueOf(jwt.getSubject());
		} catch (NumberFormatException exception) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}
	}
}
