package com.setpik.server.prestudy.controller;

import com.setpik.server.common.api.ApiResponse;
import com.setpik.server.common.config.SwaggerConfig;
import com.setpik.server.common.exception.BusinessException;
import com.setpik.server.common.exception.ErrorCode;
import com.setpik.server.prestudy.dto.CreatePrestudyPlaylistRequest;
import com.setpik.server.prestudy.dto.CreatePrestudyPlaylistResponse;
import com.setpik.server.prestudy.dto.PrestudyCandidateResponse;
import com.setpik.server.prestudy.service.PrestudyPlaylistService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/performances/{performanceId}")
@Tag(name = "예습 플레이리스트")
@SecurityRequirement(name = SwaggerConfig.BEARER_AUTH)
@Validated
public class PerformancePrestudyController {

	private final PrestudyPlaylistService prestudyPlaylistService;

	public PerformancePrestudyController(PrestudyPlaylistService prestudyPlaylistService) {
		this.prestudyPlaylistService = prestudyPlaylistService;
	}

	@GetMapping("/prestudy/candidates")
	public ApiResponse<PrestudyCandidateResponse> getCandidates(
		@PathVariable @Positive Long performanceId,
		@RequestParam @Positive Long analysisId,
		@AuthenticationPrincipal Jwt jwt
	) {
		return ApiResponse.success(prestudyPlaylistService.getCandidates(userId(jwt), performanceId, analysisId));
	}

	@PostMapping("/prestudy-playlists")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<CreatePrestudyPlaylistResponse> create(
		@PathVariable @Positive Long performanceId,
		@Valid @RequestBody CreatePrestudyPlaylistRequest request,
		@AuthenticationPrincipal Jwt jwt
	) {
		return ApiResponse.created(
			"예습 플레이리스트가 생성되었습니다.",
			prestudyPlaylistService.createPrestudyPlaylist(userId(jwt), performanceId, request)
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
