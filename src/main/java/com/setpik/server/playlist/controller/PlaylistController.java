package com.setpik.server.playlist.controller;

import com.setpik.server.common.api.ApiResponse;
import com.setpik.server.playlist.dto.PlaylistDetailResponse;
import com.setpik.server.playlist.dto.PlaylistSummaryResponse;
import com.setpik.server.playlist.dto.PlaylistSyncResponse;
import com.setpik.server.playlist.dto.TrackResponse;
import com.setpik.server.playlist.service.PlaylistService;
import java.util.List;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/playlists")
public class PlaylistController {

	// TODO: 인증 기능 병합 후 인증 주체에서 userId를 가져오도록 교체
	private static final Long TEMP_USER_ID = 1L;

	private final PlaylistService playlistService;

	public PlaylistController(PlaylistService playlistService) {
		this.playlistService = playlistService;
	}

	@PostMapping("/sync")
	public ApiResponse<PlaylistSyncResponse> sync() {
		return ApiResponse.created("플레이리스트 동기화가 완료되었습니다.",
			playlistService.sync(TEMP_USER_ID));
	}

	@GetMapping
	public ApiResponse<List<PlaylistSummaryResponse>> getMyPlaylists() {
		return ApiResponse.success(playlistService.getMyPlaylists(TEMP_USER_ID));
	}

	@GetMapping("/{playlistId}")
	public ApiResponse<PlaylistDetailResponse> getPlaylistDetail(@PathVariable Long playlistId) {
		return ApiResponse.success(playlistService.getPlaylistDetail(TEMP_USER_ID, playlistId));
	}

	@GetMapping("/{playlistId}/tracks")
	public ApiResponse<List<TrackResponse>> getPlaylistTracks(@PathVariable Long playlistId) {
		return ApiResponse.success(playlistService.getPlaylistTracks(TEMP_USER_ID, playlistId));
	}
}
