package com.setpik.server.favorite.service;

import com.setpik.server.analysis.domain.AnalysisStatus;
import com.setpik.server.analysis.repository.PlaylistAnalysisRepository;
import com.setpik.server.common.api.PageResponse;
import com.setpik.server.favorite.dto.FavoritePerformanceResponse;
import com.setpik.server.favorite.dto.FavoritePerformanceSummary;
import com.setpik.server.favorite.dto.FavoritePerformanceCreateRequest;
import com.setpik.server.favorite.dto.FavoritePerformanceCreateResponse;
import com.setpik.server.favorite.domain.FavoritePerformance;
import com.setpik.server.favorite.repository.FavoritePerformanceRepository;
import com.setpik.server.common.exception.BusinessException;
import com.setpik.server.common.exception.ErrorCode;
import com.setpik.server.performance.domain.Performance;
import com.setpik.server.performance.domain.PerformanceMatch;
import com.setpik.server.performance.repository.PerformanceMatchRepository;
import com.setpik.server.performance.repository.PerformanceRepository;
import com.setpik.server.performance.service.PerformanceMetadataLookupService;
import com.setpik.server.prestudy.dto.PrestudyPlaylistCardStatus;
import com.setpik.server.prestudy.service.PrestudyPlaylistStatusLookupService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

@Service
@Transactional(readOnly = true)
public class FavoritePerformanceService {

	private final FavoritePerformanceRepository favoriteRepository;
	private final PerformanceRepository performanceRepository;
	private final PerformanceMatchRepository performanceMatchRepository;
	private final PlaylistAnalysisRepository playlistAnalysisRepository;
	private final PerformanceMetadataLookupService performanceMetadataLookupService;
	private final PrestudyPlaylistStatusLookupService prestudyPlaylistStatusLookupService;
	private final Clock clock;

	public FavoritePerformanceService(
		FavoritePerformanceRepository favoriteRepository,
		PerformanceRepository performanceRepository,
		PerformanceMatchRepository performanceMatchRepository,
		PlaylistAnalysisRepository playlistAnalysisRepository,
		PerformanceMetadataLookupService performanceMetadataLookupService,
		PrestudyPlaylistStatusLookupService prestudyPlaylistStatusLookupService,
		Clock clock
	) {
		this.favoriteRepository = favoriteRepository;
		this.performanceRepository = performanceRepository;
		this.performanceMatchRepository = performanceMatchRepository;
		this.playlistAnalysisRepository = playlistAnalysisRepository;
		this.performanceMetadataLookupService = performanceMetadataLookupService;
		this.prestudyPlaylistStatusLookupService = prestudyPlaylistStatusLookupService;
		this.clock = clock;
	}

	public PageResponse<FavoritePerformanceResponse> getFavorites(Long userId, Pageable pageable) {
		Page<FavoritePerformanceSummary> page =
			favoriteRepository.findActiveSummariesByUserId(userId, pageable);
		List<Long> performanceIds = page.getContent().stream()
			.map(FavoritePerformanceSummary::performanceId)
			.distinct()
			.toList();
		Map<Long, String> performanceTypeByPerformanceId =
			performanceMetadataLookupService.performanceTypeCodeByPerformanceId(performanceIds);
		Map<Long, List<String>> artistNamesByPerformanceId =
			performanceMetadataLookupService.artistNamesByPerformanceId(performanceIds);
		Map<Long, Integer> matchedArtistCountByPerformanceId =
			matchedArtistCountsForLatestAnalysis(userId, performanceIds);
		Map<Long, PrestudyPlaylistCardStatus> prestudyStatusByPerformanceId =
			prestudyPlaylistStatusLookupService.latestByPerformanceId(userId, performanceIds);

		List<FavoritePerformanceResponse> content = page.getContent().stream()
			.map(summary -> FavoritePerformanceResponse.from(
				summary,
				performanceTypeByPerformanceId.get(summary.performanceId()),
				artistNamesByPerformanceId.getOrDefault(summary.performanceId(), List.of()),
				matchedArtistCountByPerformanceId.getOrDefault(summary.performanceId(), 0),
				prestudyStatusByPerformanceId.get(summary.performanceId())))
			.toList();
		return PageResponse.of(content, page);
	}

	/** 사용자의 최신 COMPLETED 분석(analyzedAt 기준) 1건과 매칭된 아티스트 수만 사용한다. */
	private Map<Long, Integer> matchedArtistCountsForLatestAnalysis(Long userId, List<Long> performanceIds) {
		if (performanceIds.isEmpty()) return Map.of();
		return playlistAnalysisRepository
			.findFirstByUserIdAndAnalysisStatusOrderByAnalyzedAtDescAnalysisIdDesc(
				userId, AnalysisStatus.COMPLETED)
			.map(analysis -> performanceMatchRepository
				.findByAnalysisIdAndPerformanceIdIn(analysis.getAnalysisId(), performanceIds).stream()
				.collect(Collectors.toMap(
					PerformanceMatch::getPerformanceId, PerformanceMatch::getMatchedArtistCount)))
			.orElse(Map.of());
	}

	@Transactional
	public FavoritePerformanceCreateResponse create(
		Long userId,
		FavoritePerformanceCreateRequest request
	) {
		Performance performance = performanceRepository
			.findByPerformanceIdAndIsDeletedFalse(request.performanceId())
			.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST));
		LocalDateTime savedAt = LocalDateTime.now(clock);

		FavoritePerformance favorite = favoriteRepository
			.findByUserIdAndPerformanceId(userId, request.performanceId())
			.orElse(null);
		if (favorite != null && favorite.getDeletedAt() == null) {
			throw new BusinessException(ErrorCode.DUPLICATE_REQUEST);
		}
		if (favorite == null) {
			favorite = new FavoritePerformance(userId, request.performanceId(), savedAt);
		} else {
			favorite.restore(savedAt);
		}

		try {
			favorite = favoriteRepository.saveAndFlush(favorite);
		} catch (DataIntegrityViolationException exception) {
			throw new BusinessException(ErrorCode.DUPLICATE_REQUEST);
		}
		performance.increaseFavoriteCount();
		return new FavoritePerformanceCreateResponse(favorite.getFavoriteId());
	}

	@Transactional
	public void delete(Long userId, Long favoriteId) {
		if (favoriteId == null || favoriteId <= 0) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}

		// 사용자 ID까지 함께 조회해 다른 회원의 관심 공연 삭제를 차단한다.
		FavoritePerformance favorite = favoriteRepository
			.findByFavoriteIdAndUserId(favoriteId, userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
		if (favorite.getDeletedAt() != null) {
			throw new BusinessException(ErrorCode.DUPLICATE_REQUEST);
		}

		Performance performance = performanceRepository.findById(favorite.getPerformanceId())
			.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
		favorite.delete(LocalDateTime.now(clock));
		performance.decreaseFavoriteCount();
	}
}
