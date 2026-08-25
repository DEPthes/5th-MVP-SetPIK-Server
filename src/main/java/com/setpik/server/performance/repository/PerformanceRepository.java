package com.setpik.server.performance.repository;

import com.setpik.server.performance.domain.Performance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 기본 CRUD는 JpaRepository가 제공하고 도메인별 조회 메서드는 여기에 추가한다. */
public interface PerformanceRepository extends JpaRepository<Performance, Long> {
	Optional<Performance> findByKopisPerformanceId(String kopisPerformanceId);
	List<Performance> findByKopisPerformanceIdIn(List<String> kopisPerformanceIds);

	Page<Performance> findByIsDeletedFalse(Pageable pageable);

	Optional<Performance> findByPerformanceIdAndIsDeletedFalse(Long performanceId);

	boolean existsByPerformanceIdAndIsDeletedFalse(Long performanceId);

	@Query("""
		select performance
		from Performance performance
		where performance.isDeleted = false
		  and (:fromDate is null or performance.endDate >= :fromDate)
		  and (:toDate is null or performance.startDate <= :toDate)
		""")
	List<Performance> findMatchCandidates(
		@Param("fromDate") LocalDate fromDate,
		@Param("toDate") LocalDate toDate
	);

	/**
	 * 아티스트명 검색은 EXISTS 서브쿼리로 처리해 Performance_Artists/Artists 조인으로 인한
	 * 행 중복(및 DISTINCT+ORDER BY를 금지하는 MySQL 제약)을 피한다.
	 */
	@Query(value = """
		select performance
		from Performance performance
		join Venue venue on venue.venueId = performance.venueId
		left join PerformanceTypeMap typeMap on typeMap.performanceId = performance.performanceId
		left join PerformanceType type on type.performanceTypeId = typeMap.performanceTypeId
		where performance.isDeleted = false
		  and (:keywordPattern is null
		       or performance.performanceName like :keywordPattern
		       or venue.venueName like :keywordPattern
		       or exists (
		            select 1
		            from PerformanceArtist lineup
		            join Artist artist on artist.artistId = lineup.artistId
		            where lineup.performanceId = performance.performanceId
		              and artist.artistName like :keywordPattern
		          ))
		  and (:performanceType is null or type.typeCode = :performanceType)
		  and (:region is null or venue.city = :region)
		  and (:fromDate is null or performance.endDate >= :fromDate)
		  and (:toDate is null or performance.startDate <= :toDate)
		""",
		countQuery = """
		select count(performance)
		from Performance performance
		join Venue venue on venue.venueId = performance.venueId
		left join PerformanceTypeMap typeMap on typeMap.performanceId = performance.performanceId
		left join PerformanceType type on type.performanceTypeId = typeMap.performanceTypeId
		where performance.isDeleted = false
		  and (:keywordPattern is null
		       or performance.performanceName like :keywordPattern
		       or venue.venueName like :keywordPattern
		       or exists (
		            select 1
		            from PerformanceArtist lineup
		            join Artist artist on artist.artistId = lineup.artistId
		            where lineup.performanceId = performance.performanceId
		              and artist.artistName like :keywordPattern
		          ))
		  and (:performanceType is null or type.typeCode = :performanceType)
		  and (:region is null or venue.city = :region)
		  and (:fromDate is null or performance.endDate >= :fromDate)
		  and (:toDate is null or performance.startDate <= :toDate)
		""")
	Page<Performance> search(
		@Param("keywordPattern") String keywordPattern,
		@Param("performanceType") String performanceType,
		@Param("region") String region,
		@Param("fromDate") LocalDate fromDate,
		@Param("toDate") LocalDate toDate,
		Pageable pageable
	);

	/**
	 * sort=recommended용: 특정 analysisId의 매치 결과(있으면)로 정렬만 하고, 매치가 없는 공연도
	 * 그대로 포함한다(matchPriority가 낮을수록/matchedArtistCount가 클수록 상위).
	 */
	@Query(value = """
		select performance
		from Performance performance
		join Venue venue on venue.venueId = performance.venueId
		left join PerformanceTypeMap typeMap on typeMap.performanceId = performance.performanceId
		left join PerformanceType type on type.performanceTypeId = typeMap.performanceTypeId
		left join PerformanceMatch match on match.performanceId = performance.performanceId
		  and match.analysisId = :analysisId
		where performance.isDeleted = false
		  and (:keywordPattern is null
		       or performance.performanceName like :keywordPattern
		       or venue.venueName like :keywordPattern
		       or exists (
		            select 1
		            from PerformanceArtist lineup
		            join Artist artist on artist.artistId = lineup.artistId
		            where lineup.performanceId = performance.performanceId
		              and artist.artistName like :keywordPattern
		          ))
		  and (:performanceType is null or type.typeCode = :performanceType)
		  and (:region is null or venue.city = :region)
		  and (:fromDate is null or performance.endDate >= :fromDate)
		  and (:toDate is null or performance.startDate <= :toDate)
		order by
		  case when match.matchPriority = 1 then 3
		       when match.matchPriority = 2 then 2
		       when match.matchPriority = 3 then 1
		       else 0 end desc,
		  case when match.matchPriority = 2 then match.matchedArtistCount else 0 end desc,
		  performance.startDate asc
		""",
		countQuery = """
		select count(performance)
		from Performance performance
		join Venue venue on venue.venueId = performance.venueId
		left join PerformanceTypeMap typeMap on typeMap.performanceId = performance.performanceId
		left join PerformanceType type on type.performanceTypeId = typeMap.performanceTypeId
		where performance.isDeleted = false
		  and (:keywordPattern is null
		       or performance.performanceName like :keywordPattern
		       or venue.venueName like :keywordPattern
		       or exists (
		            select 1
		            from PerformanceArtist lineup
		            join Artist artist on artist.artistId = lineup.artistId
		            where lineup.performanceId = performance.performanceId
		              and artist.artistName like :keywordPattern
		          ))
		  and (:performanceType is null or type.typeCode = :performanceType)
		  and (:region is null or venue.city = :region)
		  and (:fromDate is null or performance.endDate >= :fromDate)
		  and (:toDate is null or performance.startDate <= :toDate)
		""")
	Page<Performance> searchOrderedByRecommendation(
		@Param("keywordPattern") String keywordPattern,
		@Param("performanceType") String performanceType,
		@Param("region") String region,
		@Param("fromDate") LocalDate fromDate,
		@Param("toDate") LocalDate toDate,
		@Param("analysisId") Long analysisId,
		Pageable pageable
	);
}
