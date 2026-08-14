package com.setpik.server.performanceview.repository;

import com.setpik.server.performanceview.domain.PerformanceView;
import com.setpik.server.performanceview.dto.PerformanceViewSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface PerformanceViewRepository extends JpaRepository<PerformanceView, Long> {

	Optional<PerformanceView> findByUserIdAndAnalysisIdAndPerformanceId(
		Long userId,
		Long analysisId,
		Long performanceId
	);

	@Query(
		value = """
			select new com.setpik.server.performanceview.dto.PerformanceViewSummary(
				performanceView.viewId,
				performance.performanceId,
				performance.performanceName,
				performance.posterUrl,
				performance.startDate,
				venue.venueName,
				performanceView.analysisId,
				performanceView.viewedAt
			)
			from PerformanceView performanceView
			join Performance performance
				on performance.performanceId = performanceView.performanceId
			join Venue venue on venue.venueId = performance.venueId
			where performanceView.userId = :userId
			  and performance.isDeleted = false
			""",
		countQuery = """
			select count(performanceView)
			from PerformanceView performanceView
			join Performance performance
				on performance.performanceId = performanceView.performanceId
			join Venue venue on venue.venueId = performance.venueId
			where performanceView.userId = :userId
			  and performance.isDeleted = false
			"""
	)
	Page<PerformanceViewSummary> findRecentByUserId(
		@Param("userId") Long userId,
		Pageable pageable
	);
}
