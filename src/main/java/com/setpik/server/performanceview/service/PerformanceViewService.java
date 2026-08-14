package com.setpik.server.performanceview.service;

import com.setpik.server.analysis.repository.PlaylistAnalysisRepository;
import com.setpik.server.common.api.PageResponse;
import com.setpik.server.common.exception.BusinessException;
import com.setpik.server.common.exception.ErrorCode;
import com.setpik.server.performance.repository.PerformanceRepository;
import com.setpik.server.performanceview.domain.PerformanceView;
import com.setpik.server.performanceview.dto.PerformanceViewCreateRequest;
import com.setpik.server.performanceview.dto.PerformanceViewCreateResponse;
import com.setpik.server.performanceview.dto.PerformanceViewResponse;
import com.setpik.server.performanceview.dto.PerformanceViewSummary;
import com.setpik.server.performanceview.repository.PerformanceViewRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PerformanceViewService {

	private final PerformanceViewRepository performanceViewRepository;
	private final PlaylistAnalysisRepository analysisRepository;
	private final PerformanceRepository performanceRepository;
	private final Clock clock;

	public PerformanceViewService(
		PerformanceViewRepository performanceViewRepository,
		PlaylistAnalysisRepository analysisRepository,
		PerformanceRepository performanceRepository,
		Clock clock
	) {
		this.performanceViewRepository = performanceViewRepository;
		this.analysisRepository = analysisRepository;
		this.performanceRepository = performanceRepository;
		this.clock = clock;
	}

	public PageResponse<PerformanceViewResponse> getRecentViews(Long userId, Pageable pageable) {
		Page<PerformanceViewSummary> page =
			performanceViewRepository.findRecentByUserId(userId, pageable);
		List<PerformanceViewResponse> content = page.getContent().stream()
			.map(PerformanceViewResponse::from)
			.toList();
		return PageResponse.of(content, page);
	}

	@Transactional
	public PerformanceViewCreateResponse saveOrUpdate(
		Long userId,
		PerformanceViewCreateRequest request
	) {
		if (!analysisRepository.existsByAnalysisIdAndUserId(request.analysisId(), userId)) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
		}
		if (!performanceRepository.existsByPerformanceIdAndIsDeletedFalse(request.performanceId())) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
		}

		LocalDateTime viewedAt = LocalDateTime.now(clock);
		PerformanceView performanceView = performanceViewRepository
			.findByUserIdAndAnalysisIdAndPerformanceId(
				userId, request.analysisId(), request.performanceId())
			.orElse(null);
		boolean created = performanceView == null;
		if (created) {
			performanceView = new PerformanceView(
				userId, request.analysisId(), request.performanceId(), viewedAt);
			performanceView = performanceViewRepository.save(performanceView);
		} else {
			performanceView.updateViewedAt(viewedAt);
		}

		return PerformanceViewCreateResponse.of(
			performanceView.getViewId(), created, performanceView.getViewedAt());
	}
}
