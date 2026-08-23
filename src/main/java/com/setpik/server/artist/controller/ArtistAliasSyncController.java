package com.setpik.server.artist.controller;

import com.setpik.server.artist.config.ArtistAliasSyncProperties;
import com.setpik.server.artist.dto.ArtistAliasSyncResponse;
import com.setpik.server.artist.service.ArtistAliasSyncService;
import com.setpik.server.common.api.ApiResponse;
import com.setpik.server.common.config.SwaggerConfig;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/artist-aliases")
@Tag(name = "아티스트 Alias 내부 연동", description = "검증된 외부 식별자로 KOPIS 출연진 Alias를 자동 생성하는 API")
@SecurityRequirement(name = SwaggerConfig.BEARER_AUTH)
public class ArtistAliasSyncController {

	private final ArtistAliasSyncService syncService;
	private final ArtistAliasSyncProperties properties;

	public ArtistAliasSyncController(ArtistAliasSyncService syncService, ArtistAliasSyncProperties properties) {
		this.syncService = syncService;
		this.properties = properties;
	}

	@PostMapping("/sync")
	public ApiResponse<ArtistAliasSyncResponse> syncAliases(
		@RequestParam(required = false) Integer limit
	) {
		int batchSize = limit == null ? properties.getBatchSize() : limit;
		return ApiResponse.success("검증된 KOPIS 출연진 Alias 동기화가 완료되었습니다.",
			syncService.syncPendingAliases(batchSize));
	}
}
