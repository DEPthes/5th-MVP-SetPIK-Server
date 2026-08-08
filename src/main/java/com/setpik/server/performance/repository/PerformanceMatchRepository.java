package com.setpik.server.performance.repository;

import com.setpik.server.performance.domain.PerformanceMatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 기본 CRUD는 JpaRepository가 제공하고 도메인별 조회 메서드는 여기에 추가한다. */
public interface PerformanceMatchRepository extends JpaRepository<PerformanceMatch, Long> {

	@Query("""
		select match
		from PerformanceMatch match
		where match.analysisId = :analysisId
		  and match.performanceId in (
		    select performance.performanceId
		    from Performance performance
		    where performance.isDeleted = false
		  )
		""")
	Page<PerformanceMatch> findVisibleByAnalysisId(
		@Param("analysisId") Long analysisId,
		Pageable pageable
	);

	Optional<PerformanceMatch> findByAnalysisIdAndPerformanceId(Long analysisId, Long performanceId);

	List<PerformanceMatch> findAllByAnalysisId(Long analysisId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("delete from PerformanceMatch match where match.analysisId = :analysisId")
	void deleteAllByAnalysisId(@Param("analysisId") Long analysisId);
}
