package com.setpik.server.performance.repository;

import com.setpik.server.performance.domain.PerformanceMatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** 기본 CRUD는 JpaRepository가 제공하고 도메인별 조회 메서드는 여기에 추가한다. */
public interface PerformanceMatchRepository extends JpaRepository<PerformanceMatch, Long> {

	Page<PerformanceMatch> findByAnalysisIdOrderByMatchPriorityAsc(Long analysisId, Pageable pageable);

	Optional<PerformanceMatch> findByAnalysisIdAndPerformanceId(Long analysisId, Long performanceId);
}