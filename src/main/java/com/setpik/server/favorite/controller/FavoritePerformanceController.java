package com.setpik.server.favorite.controller;

import com.setpik.server.common.api.ApiResponse;
import com.setpik.server.common.api.PageResponse;
import com.setpik.server.common.config.SwaggerConfig;
import com.setpik.server.common.exception.BusinessException;
import com.setpik.server.common.exception.ErrorCode;
import com.setpik.server.favorite.dto.FavoritePerformanceResponse;
import com.setpik.server.favorite.dto.FavoritePerformanceCreateRequest;
import com.setpik.server.favorite.dto.FavoritePerformanceCreateResponse;
import com.setpik.server.favorite.service.FavoritePerformanceService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/favorites")
@Tag(name = "관심 공연", description = "회원 관심 공연 API")
@SecurityRequirement(name = SwaggerConfig.BEARER_AUTH)
public class FavoritePerformanceController {

	private static final int MAX_PAGE_SIZE = 100;
	private final FavoritePerformanceService favoriteService;

	public FavoritePerformanceController(FavoritePerformanceService favoriteService) {
		this.favoriteService = favoriteService;
	}

	@GetMapping
	public ApiResponse<PageResponse<FavoritePerformanceResponse>> getFavorites(
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size,
		@RequestParam(defaultValue = "savedAt,desc") String sort,
		@AuthenticationPrincipal Jwt jwt
	) {
		return ApiResponse.success(
			favoriteService.getFavorites(userId(jwt), toPageable(page, size, sort)));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<FavoritePerformanceCreateResponse> create(
		@Valid @RequestBody FavoritePerformanceCreateRequest request,
		@AuthenticationPrincipal Jwt jwt
	) {
		return ApiResponse.created(
			"관심 공연이 저장되었습니다.",
			favoriteService.create(userId(jwt), request)
		);
	}

	@DeleteMapping("/{favoriteId}")
	public ApiResponse<Void> delete(
		@PathVariable Long favoriteId,
		@AuthenticationPrincipal Jwt jwt
	) {
		favoriteService.delete(userId(jwt), favoriteId);
		return ApiResponse.success("관심 공연이 삭제되었습니다.", null);
	}

	private Pageable toPageable(int page, int size, String sort) {
		if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}

		String[] parts = sort == null ? new String[0] : sort.trim().split(",");
		if (parts.length != 2 || !"savedAt".equals(parts[0])) {
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
