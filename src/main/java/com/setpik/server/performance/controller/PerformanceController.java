package com.setpik.server.performance.controller;

import com.setpik.server.common.api.ApiResponse;
import com.setpik.server.common.api.PageResponse;
import com.setpik.server.performance.dto.PerformanceArtistResponse;
import com.setpik.server.performance.dto.PerformanceDetailResponse;
import com.setpik.server.performance.dto.PerformanceSummaryResponse;
import com.setpik.server.performance.dto.TicketScheduleResponse;
import com.setpik.server.performance.service.PerformanceService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/performances")
@Tag(name = "공연", description = "공연 조회 API")
public class PerformanceController {

	private final PerformanceService performanceService;

	public PerformanceController(PerformanceService performanceService) {
		this.performanceService = performanceService;
	}

	@GetMapping
	public ApiResponse<PageResponse<PerformanceSummaryResponse>> getPerformances(
		@RequestParam(defaultValue = "0") @Min(0) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
	) {
		return ApiResponse.success(performanceService.getPerformances(page, size));
	}

	@GetMapping("/{performanceId}")
	public ApiResponse<PerformanceDetailResponse> getPerformance(@PathVariable Long performanceId) {
		return ApiResponse.success(performanceService.getPerformance(performanceId));
	}

	@GetMapping("/{performanceId}/ticket-schedules")
	public ApiResponse<List<TicketScheduleResponse>> getTicketSchedules(@PathVariable Long performanceId) {
		return ApiResponse.success(performanceService.getTicketSchedules(performanceId));
	}

	@GetMapping("/{performanceId}/artists")
	public ApiResponse<List<PerformanceArtistResponse>> getArtists(@PathVariable Long performanceId) {
		return ApiResponse.success(performanceService.getArtists(performanceId));
	}
}