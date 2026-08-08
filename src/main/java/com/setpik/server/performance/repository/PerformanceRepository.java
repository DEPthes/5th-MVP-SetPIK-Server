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
}
