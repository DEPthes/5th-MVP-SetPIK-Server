package com.setpik.server.playlist.controller;

import com.setpik.server.common.api.ApiResponse;
import com.setpik.server.common.config.SwaggerConfig;
import com.setpik.server.common.exception.BusinessException;
import com.setpik.server.common.exception.ErrorCode;
import com.setpik.server.playlist.dto.PlaylistDetailResponse;
import com.setpik.server.playlist.dto.PlaylistPageResponse;
import com.setpik.server.playlist.dto.PlaylistSyncResponse;
import com.setpik.server.playlist.dto.TrackPageResponse;
import com.setpik.server.playlist.service.PlaylistService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.setpik.server.common.api.PageResponse;
import com.setpik.server.playlist.dto.PlaylistSelectResponse;
import com.setpik.server.playlist.dto.RecentSelectionResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@RestController
@RequestMapping("/api/v1/playlists")
@SecurityRequirement(name = SwaggerConfig.BEARER_AUTH)
public class PlaylistController {
	private final PlaylistService playlistService;

	public PlaylistController(PlaylistService playlistService) {
		this.playlistService = playlistService;
	}

	@PostMapping("/sync")
	public ApiResponse<PlaylistSyncResponse> sync(@AuthenticationPrincipal Jwt jwt) {
		return ApiResponse.success("플레이리스트 동기화가 완료되었습니다.",
			playlistService.sync(userId(jwt)));
	}
	@PostMapping("/{playlistId}/select")
	public ApiResponse<PlaylistSelectResponse> select(
		@PathVariable Long playlistId,
		@AuthenticationPrincipal Jwt jwt
	) {
		return ApiResponse.created("플레이리스트가 선택되었습니다.",
			playlistService.select(userId(jwt), playlistId));
	}

	@GetMapping("/recent-selections")
	public ApiResponse<PageResponse<RecentSelectionResponse>> getRecentSelections(
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size,
		@RequestParam(defaultValue = "selectedAt,desc") String sort,
		@AuthenticationPrincipal Jwt jwt
	) {
		return ApiResponse.success(
			playlistService.getRecentSelections(userId(jwt), toPageable(page, size, sort)));
	}

	private Pageable toPageable(int page, int size, String sort) {
		if (page < 0 || size < 1 || size > 100) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}

		String[] parts = sort == null ? new String[0] : sort.trim().split(",");
		if (parts.length != 2) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}

		try {
			return PageRequest.of(page, size,
				Sort.by(Sort.Direction.fromString(parts[1]), parts[0]));
		} catch (IllegalArgumentException exception) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}
	}

	@GetMapping
	public ApiResponse<PlaylistPageResponse> getMyPlaylists(
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size,
		@RequestParam(defaultValue = "playlistName,asc") String sort,
		@RequestParam(required = false) String keyword,
		@AuthenticationPrincipal Jwt jwt
	) {
		return ApiResponse.success(
			playlistService.getMyPlaylists(userId(jwt), page, size, sort, keyword)
		);
	}

	@GetMapping("/{playlistId}")
	public ApiResponse<PlaylistDetailResponse> getPlaylistDetail(
		@PathVariable Long playlistId,
		@AuthenticationPrincipal Jwt jwt
	) {
		return ApiResponse.success(playlistService.getPlaylistDetail(userId(jwt), playlistId));
	}

	@GetMapping("/{playlistId}/tracks")
	public ApiResponse<TrackPageResponse> getPlaylistTracks(
		@PathVariable Long playlistId,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size,
		@RequestParam(defaultValue = "trackPosition,asc") String sort,
		@AuthenticationPrincipal Jwt jwt
	) {
		return ApiResponse.success(
			playlistService.getPlaylistTracks(userId(jwt), playlistId, page, size, sort)
		);
	}

	private Long userId(Jwt jwt) {
		try {
			return Long.valueOf(jwt.getSubject());
		} catch (NumberFormatException exception) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}
	}
}
