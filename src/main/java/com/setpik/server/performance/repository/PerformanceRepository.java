package com.setpik.server.performance.repository;

import com.setpik.server.performance.domain.Performance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** 기본 CRUD는 JpaRepository가 제공하고 도메인별 조회 메서드는 여기에 추가한다. */
public interface PerformanceRepository extends JpaRepository<Performance, Long> {

	Page<Performance> findByIsDeletedFalse(Pageable pageable);

	Optional<Performance> findByPerformanceIdAndIsDeletedFalse(Long performanceId);

	boolean existsByPerformanceIdAndIsDeletedFalse(Long performanceId);
}