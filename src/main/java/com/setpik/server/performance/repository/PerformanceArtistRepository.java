package com.setpik.server.performance.repository;

import com.setpik.server.performance.domain.PerformanceArtist;
import com.setpik.server.performance.domain.PerformanceArtistId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** 기본 CRUD는 JpaRepository가 제공하고 도메인별 조회 메서드는 여기에 추가한다. */
public interface PerformanceArtistRepository extends JpaRepository<PerformanceArtist, PerformanceArtistId> {

	List<PerformanceArtist> findByPerformanceIdOrderByLineupOrderAsc(Long performanceId);
}