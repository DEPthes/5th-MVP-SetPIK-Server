package com.setpik.server.performanceview.controller;

import com.setpik.server.common.api.ApiResponse;
import com.setpik.server.common.api.PageResponse;
import com.setpik.server.common.config.SwaggerConfig;
import com.setpik.server.common.exception.BusinessException;
import com.setpik.server.common.exception.ErrorCode;
import com.setpik.server.performanceview.dto.PerformanceViewResponse;
import com.setpik.server.performanceview.dto.PerformanceViewCreateRequest;
import com.setpik.server.performanceview.dto.PerformanceViewCreateResponse;
import com.setpik.server.performanceview.service.PerformanceViewService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/performance-views")
@Tag(name = "공연 조회 이력", description = "회원의 최근 공연 조회 이력 API")
@SecurityRequirement(name = SwaggerConfig.BEARER_AUTH)
public class PerformanceViewController {

	private static final int MAX_PAGE_SIZE = 100;
	private final PerformanceViewService performanceViewService;

	public PerformanceViewController(PerformanceViewService performanceViewService) {
		this.performanceViewService = performanceViewService;
	}

	@GetMapping
	public ApiResponse<PageResponse<PerformanceViewResponse>> getRecentViews(
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size,
		@RequestParam(defaultValue = "viewedAt,desc") String sort,
		@AuthenticationPrincipal Jwt jwt
	) {
		return ApiResponse.success(
			performanceViewService.getRecentViews(userId(jwt), toPageable(page, size, sort)));
	}

	@PostMapping
	public ApiResponse<PerformanceViewCreateResponse> saveOrUpdate(
		@Valid @RequestBody PerformanceViewCreateRequest request,
		@AuthenticationPrincipal Jwt jwt
	) {
		return ApiResponse.success(
			"조회 기록이 저장 또는 갱신되었습니다.",
			performanceViewService.saveOrUpdate(userId(jwt), request)
		);
	}

	private Pageable toPageable(int page, int size, String sort) {
		if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}

		String[] parts = sort == null ? new String[0] : sort.trim().split(",");
		if (parts.length != 2 || !"viewedAt".equals(parts[0])) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}

		try {
			return PageRequest.of(page, size,
				Sort.by(Sort.Direction.fromString(parts[1]), parts[0]));
		} catch (IllegalArgumentException exception) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}
	}

	private Long userId(Jwt jwt) {
		try {
			return Long.valueOf(jwt.getSubject());
		} catch (NumberFormatException exception) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}
	}
}
