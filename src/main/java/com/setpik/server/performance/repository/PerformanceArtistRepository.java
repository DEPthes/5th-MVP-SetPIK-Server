package com.setpik.server.performance.repository;

import com.setpik.server.performance.domain.PerformanceArtist;
import com.setpik.server.performance.domain.PerformanceArtistId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 기본 CRUD는 JpaRepository가 제공하고 도메인별 조회 메서드는 여기에 추가한다. */
public interface PerformanceArtistRepository extends JpaRepository<PerformanceArtist, PerformanceArtistId> {

	List<PerformanceArtist> findByPerformanceIdOrderByLineupOrderAsc(Long performanceId);

	List<PerformanceArtist> findByPerformanceIdIn(List<Long> performanceIds);
	void deleteByPerformanceId(Long performanceId);

	@Modifying
	@Query("delete from PerformanceArtist mapping where mapping.performanceId in :performanceIds")
	void deleteByPerformanceIdIn(@Param("performanceIds") List<Long> performanceIds);

	/**
	 * KOPIS에서 저장된 한글 출연진 매핑을 이미 존재하는 Spotify 아티스트로 복사한다.
	 * 음악 공연만 대상으로 하며, 같은 공연에 대상 아티스트가 이미 있으면 중복 삽입하지 않는다.
	 */
	@Modifying
	@Query(value = """
		insert ignore into Performance_Artists (artist_id, performance_id, lineup_order, is_headliner)
		select :targetArtistId, mapping.performance_id, mapping.lineup_order, mapping.is_headliner
		from Performance_Artists mapping
		join Performances performance on performance.performance_id = mapping.performance_id
		join Performance_Genres performanceGenre on performanceGenre.performance_id = performance.performance_id
		join Genres genre on genre.genre_id = performanceGenre.genre_id
		where mapping.artist_id = :sourceArtistId
		  and performance.is_deleted = false
		  and genre.genre_name like concat('%', '음악', '%')
		""", nativeQuery = true)
	int copyMusicPerformanceMappings(
		@Param("sourceArtistId") Long sourceArtistId,
		@Param("targetArtistId") Long targetArtistId
	);

	/** 대상 Spotify 아티스트로 복사한 음악 공연의 기존 KOPIS 출연진 매핑만 제거한다. */
	@Modifying
	@Query(value = """
		delete mapping
		from Performance_Artists mapping
		join Performances performance on performance.performance_id = mapping.performance_id
		join Performance_Genres performanceGenre on performanceGenre.performance_id = performance.performance_id
		join Genres genre on genre.genre_id = performanceGenre.genre_id
		where mapping.artist_id = :sourceArtistId
		  and performance.is_deleted = false
		  and genre.genre_name like concat('%', '음악', '%')
		""", nativeQuery = true)
	void deleteMusicPerformanceMappings(@Param("sourceArtistId") Long sourceArtistId);
}
