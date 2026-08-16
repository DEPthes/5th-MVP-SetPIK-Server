package com.setpik.server.performance.repository;

import com.setpik.server.performance.domain.PerformanceGenre;
import com.setpik.server.performance.domain.PerformanceGenreId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 기본 CRUD는 JpaRepository가 제공하고 도메인별 조회 메서드는 여기에 추가한다. */
public interface PerformanceGenreRepository extends JpaRepository<PerformanceGenre, PerformanceGenreId> {

	List<PerformanceGenre> findByPerformanceIdIn(List<Long> performanceIds);
	void deleteByPerformanceId(Long performanceId);

	@Modifying
	@Query("delete from PerformanceGenre mapping where mapping.performanceId in :performanceIds")
	void deleteByPerformanceIdIn(@Param("performanceIds") List<Long> performanceIds);
}
