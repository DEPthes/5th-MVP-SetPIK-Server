package com.setpik.server.performance.repository;

import com.setpik.server.performance.domain.PerformanceMatchArtist;
import com.setpik.server.performance.domain.PerformanceMatchArtistId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 기본 CRUD는 JpaRepository가 제공하고 도메인별 조회 메서드는 여기에 추가한다. */
public interface PerformanceMatchArtistRepository extends JpaRepository<PerformanceMatchArtist, PerformanceMatchArtistId> {

	List<PerformanceMatchArtist> findByMatchId(Long matchId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("delete from PerformanceMatchArtist artist where artist.matchId in :matchIds")
	void deleteAllByMatchIdIn(@Param("matchIds") List<Long> matchIds);
}
