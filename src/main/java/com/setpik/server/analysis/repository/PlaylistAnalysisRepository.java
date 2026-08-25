package com.setpik.server.analysis.repository;

import com.setpik.server.analysis.domain.PlaylistAnalysis;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 기본 CRUD는 JpaRepository가 제공하고 도메인별 조회 메서드는 여기에 추가한다. */
public interface PlaylistAnalysisRepository extends JpaRepository<PlaylistAnalysis, Long> {

	Optional<PlaylistAnalysis> findByAnalysisIdAndUserId(Long analysisId, Long userId);
	Optional<PlaylistAnalysis> findFirstByPlaylistIdAndUserIdOrderByAnalyzedAtDescAnalysisIdDesc(
		Long playlistId, Long userId);
	Optional<PlaylistAnalysis> findFirstByUserIdOrderByAnalyzedAtDescAnalysisIdDesc(Long userId);

	boolean existsByAnalysisIdAndUserId(Long analysisId, Long userId);
}
