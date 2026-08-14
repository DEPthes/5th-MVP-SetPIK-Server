package com.setpik.server.favorite.service;

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
import com.setpik.server.performance.repository.PerformanceRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
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
	private final Clock clock;

	public FavoritePerformanceService(
		FavoritePerformanceRepository favoriteRepository,
		PerformanceRepository performanceRepository,
		Clock clock
	) {
		this.favoriteRepository = favoriteRepository;
		this.performanceRepository = performanceRepository;
		this.clock = clock;
	}

	public PageResponse<FavoritePerformanceResponse> getFavorites(Long userId, Pageable pageable) {
		Page<FavoritePerformanceSummary> page =
			favoriteRepository.findActiveSummariesByUserId(userId, pageable);
		List<FavoritePerformanceResponse> content = page.getContent().stream()
			.map(FavoritePerformanceResponse::from)
			.toList();
		return PageResponse.of(content, page);
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
