package com.setpik.server.artist.controller;

import com.setpik.server.artist.config.ArtistGenreSyncProperties;
import com.setpik.server.artist.dto.ArtistGenreSyncResponse;
import com.setpik.server.artist.service.ArtistGenreSyncService;
import com.setpik.server.common.api.ApiResponse;
import com.setpik.server.common.config.SwaggerConfig;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/artist-genres")
@Tag(name = "아티스트 장르 내부 연동", description = "외부 메타데이터로 Spotify/KOPIS 아티스트의 세부 장르를 보강하는 API")
@SecurityRequirement(name = SwaggerConfig.BEARER_AUTH)
public class ArtistGenreSyncController {
	private final ArtistGenreSyncService service;
	private final ArtistGenreSyncProperties properties;

	public ArtistGenreSyncController(ArtistGenreSyncService service, ArtistGenreSyncProperties properties) {
		this.service = service;
		this.properties = properties;
	}

	@PostMapping("/sync")
	public ApiResponse<ArtistGenreSyncResponse> sync(@RequestParam(required = false) Integer limit) {
		return ApiResponse.success("아티스트 세부 장르 동기화가 완료되었습니다.",
			service.syncPendingGenres(limit == null ? properties.getBatchSize() : limit));
	}
}
