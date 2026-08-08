package com.setpik.server.performance.controller;

import com.setpik.server.common.api.ApiResponse;
import com.setpik.server.performance.dto.PerformanceDetailResponse;
import com.setpik.server.performance.dto.TicketScheduleResponse;
import com.setpik.server.performance.service.PerformanceService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
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
}
