package com.setpik.server.analysis.controller;

import com.setpik.server.analysis.dto.AnalysisArtistResponse;
import com.setpik.server.analysis.dto.AnalysisArtistUpdateRequest;
import com.setpik.server.analysis.dto.AnalysisArtistUpdateResponse;
import com.setpik.server.analysis.dto.AnalysisDetailResponse;
import com.setpik.server.analysis.dto.AnalysisResponse;
import com.setpik.server.analysis.dto.ArtistSelectionCompleteResponse;
import com.setpik.server.analysis.service.AnalysisService;
import com.setpik.server.common.api.ApiResponse;
import com.setpik.server.common.api.PageResponse;
import com.setpik.server.common.config.SwaggerConfig;
import com.setpik.server.common.exception.BusinessException;
import com.setpik.server.common.exception.ErrorCode;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;
import java.util.Set;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = SwaggerConfig.BEARER_AUTH)
public class AnalysisController {

	private static final int MAX_PAGE_SIZE = 100;
	private static final Set<String> ANALYSIS_ARTIST_SORT_FIELDS = Set.of(
		"displayRank", "occurrenceCount", "popularitySnapshot", "artistId");

	private final AnalysisService analysisService;

	public AnalysisController(AnalysisService analysisService) {
		this.analysisService = analysisService;
	}

	@PostMapping("/playlists/{playlistId}/analysis")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<AnalysisResponse> analyze(
		@PathVariable Long playlistId,
		@AuthenticationPrincipal Jwt jwt
	) {
		return ApiResponse.created("플레이리스트 분석이 완료되었습니다.",
			analysisService.analyze(userId(jwt), playlistId));
	}

	@GetMapping("/playlists/{playlistId}/analysis")
	public ApiResponse<AnalysisDetailResponse> getLatestAnalysis(
		@PathVariable Long playlistId,
		@AuthenticationPrincipal Jwt jwt
	) {
		return ApiResponse.success(analysisService.getLatestAnalysis(userId(jwt), playlistId));
	}

	@PatchMapping("/analyses/{analysisId}/artists")
	public ApiResponse<AnalysisArtistUpdateResponse> updateArtistExclusion(
		@PathVariable Long analysisId,
		@Valid @RequestBody AnalysisArtistUpdateRequest request,
		@AuthenticationPrincipal Jwt jwt
	) {
		return ApiResponse.success("분석 아티스트 상태가 수정되었습니다.",
			analysisService.updateArtistExclusion(userId(jwt), analysisId, request));
	}

	@GetMapping("/analyses/{analysisId}/artists")
	public ApiResponse<PageResponse<AnalysisArtistResponse>> getAnalysisArtists(
		@PathVariable Long analysisId,
		@RequestParam(defaultValue = "false") boolean includeExcluded,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size,
		@RequestParam(defaultValue = "displayRank,asc") String sort,
		@AuthenticationPrincipal Jwt jwt
	) {
		return ApiResponse.success(analysisService.getAnalysisArtists(
			userId(jwt), analysisId, includeExcluded, toPageable(page, size, sort)));
	}

	@PostMapping("/analyses/{analysisId}/artist-selection/complete")
	public ApiResponse<ArtistSelectionCompleteResponse> completeArtistSelection(
		@PathVariable Long analysisId,
		@AuthenticationPrincipal Jwt jwt
	) {
		return ApiResponse.success("관심 아티스트 선택이 완료되었습니다.",
			analysisService.completeArtistSelection(userId(jwt), analysisId));
	}

	private Pageable toPageable(int page, int size, String sort) {
		if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}

		String[] parts = sort == null ? new String[0] : sort.trim().split(",");
		if (parts.length != 2 || !ANALYSIS_ARTIST_SORT_FIELDS.contains(parts[0])) {
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
