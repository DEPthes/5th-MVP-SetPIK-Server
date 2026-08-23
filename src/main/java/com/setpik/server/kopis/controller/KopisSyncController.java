package com.setpik.server.kopis.controller;

import com.setpik.server.common.api.ApiResponse;
import com.setpik.server.common.config.SwaggerConfig;
import com.setpik.server.kopis.dto.KopisSyncResponse;
import com.setpik.server.kopis.dto.KopisArtistBackfillResponse;
import com.setpik.server.kopis.service.KopisArtistBackfillService;
import com.setpik.server.kopis.service.KopisPerformanceSyncService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/kopis")
@Tag(name = "KOPIS 내부 연동", description = "운영자가 KOPIS 공연 데이터를 내부 DB로 동기화하는 API")
@SecurityRequirement(name = SwaggerConfig.BEARER_AUTH)
public class KopisSyncController {

	private final KopisPerformanceSyncService syncService;
	private final KopisArtistBackfillService artistBackfillService;

	public KopisSyncController(
		KopisPerformanceSyncService syncService,
		KopisArtistBackfillService artistBackfillService
	) {
		this.syncService = syncService;
		this.artistBackfillService = artistBackfillService;
	}

	@PostMapping("/performances/sync")
	public ApiResponse<KopisSyncResponse> syncPerformances(
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
	) {
		return ApiResponse.success("KOPIS 공연 동기화가 완료되었습니다.",
			syncService.sync(fromDate, toDate));
	}

	@PostMapping("/artists/spotify-mapping/backfill")
	public ApiResponse<KopisArtistBackfillResponse> backfillArtistSpotifyMappings(
		@RequestParam(required = false) Long afterArtistId,
		@RequestParam(defaultValue = "10") int limit
	) {
		return ApiResponse.success("KOPIS 출연진 Spotify 아티스트 재연결이 완료되었습니다.",
			artistBackfillService.backfill(afterArtistId, limit));
	}
}
