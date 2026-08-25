package com.setpik.server.favorite.repository;

import com.setpik.server.favorite.domain.FavoritePerformance;
import com.setpik.server.favorite.dto.FavoritePerformanceSummary;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

/** 기본 CRUD는 JpaRepository가 제공하고 도메인별 조회 메서드는 여기에 추가한다. */
public interface FavoritePerformanceRepository extends JpaRepository<FavoritePerformance, Long> {

	Optional<FavoritePerformance> findByUserIdAndPerformanceId(Long userId, Long performanceId);

	Optional<FavoritePerformance> findByFavoriteIdAndUserId(Long favoriteId, Long userId);

	List<FavoritePerformance> findAllByUserIdAndDeletedAtIsNull(Long userId);

	@Query(
		value = """
			select new com.setpik.server.favorite.dto.FavoritePerformanceSummary(
				favorite.favoriteId,
				performance.performanceId,
				performance.performanceName,
				performance.posterUrl,
				performance.startDate,
				venue.venueName,
				performance.performanceStatus,
				performance.priceType,
				performance.ticketPriceText,
				favorite.savedAt
			)
			from FavoritePerformance favorite
			join Performance performance on performance.performanceId = favorite.performanceId
			join Venue venue on venue.venueId = performance.venueId
			where favorite.userId = :userId
			  and favorite.deletedAt is null
			  and performance.isDeleted = false
			""",
		countQuery = """
			select count(favorite)
			from FavoritePerformance favorite
			join Performance performance on performance.performanceId = favorite.performanceId
			join Venue venue on venue.venueId = performance.venueId
			where favorite.userId = :userId
			  and favorite.deletedAt is null
			  and performance.isDeleted = false
			"""
	)
	Page<FavoritePerformanceSummary> findActiveSummariesByUserId(
		@Param("userId") Long userId,
		Pageable pageable
	);
}
