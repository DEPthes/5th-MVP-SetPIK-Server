package com.setpik.server.favorite.repository;

import com.setpik.server.favorite.domain.FavoritePerformance;
import org.springframework.data.jpa.repository.JpaRepository;

/** 기본 CRUD는 JpaRepository가 제공하고 도메인별 조회 메서드는 여기에 추가한다. */
public interface FavoritePerformanceRepository extends JpaRepository<FavoritePerformance, Long> {
}
