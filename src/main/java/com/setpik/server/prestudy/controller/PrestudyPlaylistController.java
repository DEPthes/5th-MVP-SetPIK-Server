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

import java.util.List;

@RestController
@RequestMapping("/api/v1/prestudy-playlists")
@Tag(name = "예습 플레이리스트")
@SecurityRequirement(name = SwaggerConfig.BEARER_AUTH)
public class PrestudyPlaylistController {

	private final PrestudyPlaylistService prestudyPlaylistService;

	public PrestudyPlaylistController(PrestudyPlaylistService prestudyPlaylistService) {
		this.prestudyPlaylistService = prestudyPlaylistService;
	}

	@GetMapping
	public ApiResponse<PageResponse<PrestudyPlaylistSummaryResponse>> getMyPlaylists(
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size,
		@AuthenticationPrincipal Jwt jwt
	) {
		return ApiResponse.success(prestudyPlaylistService.getMyPrestudyPlaylists(userId(jwt), page, size));
	}

	@GetMapping("/{prestudyPlaylistId}")
	public ApiResponse<PrestudyPlaylistDetailResponse> getPlaylist(
		@PathVariable Long prestudyPlaylistId,
		@AuthenticationPrincipal Jwt jwt
	) {
		return ApiResponse.success(prestudyPlaylistService.getPrestudyPlaylist(userId(jwt), prestudyPlaylistId));
	}

	@GetMapping("/{prestudyPlaylistId}/tracks")
	public ApiResponse<List<PrestudyPlaylistTrackResponse>> getTracks(
		@PathVariable Long prestudyPlaylistId,
		@AuthenticationPrincipal Jwt jwt
	) {
		return ApiResponse.success(prestudyPlaylistService.getPrestudyPlaylistTracks(userId(jwt), prestudyPlaylistId));
	}

	private Long userId(Jwt jwt) {
		try {
			return Long.valueOf(jwt.getSubject());
		} catch (NumberFormatException exception) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}
	}
}