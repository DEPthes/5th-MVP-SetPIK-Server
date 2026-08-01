package com.setpik.server.performance.repository;

import com.setpik.server.performance.domain.PerformanceGenre;
import com.setpik.server.performance.domain.PerformanceGenreId;
import org.springframework.data.jpa.repository.JpaRepository;

/** 기본 CRUD는 JpaRepository가 제공하고 도메인별 조회 메서드는 여기에 추가한다. */
public interface PerformanceGenreRepository extends JpaRepository<PerformanceGenre, PerformanceGenreId> {
}
