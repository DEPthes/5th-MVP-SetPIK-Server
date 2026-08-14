package com.setpik.server.prestudy.controller;

import com.setpik.server.common.api.ApiResponse;
import com.setpik.server.common.api.PageResponse;
import com.setpik.server.common.config.SwaggerConfig;
import com.setpik.server.common.exception.BusinessException;
import com.setpik.server.common.exception.ErrorCode;
import com.setpik.server.prestudy.dto.PrestudyPlaylistDetailResponse;
import com.setpik.server.prestudy.dto.PrestudyPlaylistSummaryResponse;
import com.setpik.server.prestudy.dto.PrestudyPlaylistTrackResponse;
import com.setpik.server.prestudy.service.PrestudyPlaylistService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Positive;

import java.util.List;

@RestController
@RequestMapping("/api/v1/prestudy-playlists")
@Tag(name = "예습 플레이리스트")
@SecurityRequirement(name = SwaggerConfig.BEARER_AUTH)
@Validated
public class PrestudyPlaylistController {

	private static final int MAX_PAGE_SIZE = 100;
	private final PrestudyPlaylistService prestudyPlaylistService;

	public PrestudyPlaylistController(PrestudyPlaylistService prestudyPlaylistService) {
		this.prestudyPlaylistService = prestudyPlaylistService;
	}

	@GetMapping
	public ApiResponse<PageResponse<PrestudyPlaylistSummaryResponse>> getMyPlaylists(
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size,
		@RequestParam(defaultValue = "createdAt,desc") String sort,
		@AuthenticationPrincipal Jwt jwt
	) {
		return ApiResponse.success(
			prestudyPlaylistService.getMyPrestudyPlaylists(userId(jwt), toPageable(page, size, sort)));
	}

	@GetMapping("/{prestudyPlaylistId}")
	public ApiResponse<PrestudyPlaylistDetailResponse> getPlaylist(
		@PathVariable @Positive Long prestudyPlaylistId,
		@AuthenticationPrincipal Jwt jwt
	) {
		return ApiResponse.success(prestudyPlaylistService.getPrestudyPlaylist(userId(jwt), prestudyPlaylistId));
	}

	@GetMapping("/{prestudyPlaylistId}/tracks")
	public ApiResponse<List<PrestudyPlaylistTrackResponse>> getTracks(
		@PathVariable @Positive Long prestudyPlaylistId,
		@AuthenticationPrincipal Jwt jwt
	) {
		return ApiResponse.success(prestudyPlaylistService.getPrestudyPlaylistTracks(userId(jwt), prestudyPlaylistId));
	}

	private Pageable toPageable(int page, int size, String sort) {
		if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}

		String[] parts = sort == null ? new String[0] : sort.trim().split(",");
		if (parts.length != 2 || !"createdAt".equals(parts[0])) {
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
