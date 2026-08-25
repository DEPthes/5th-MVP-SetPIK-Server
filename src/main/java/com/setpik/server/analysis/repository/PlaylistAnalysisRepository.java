package com.setpik.server.analysis.repository;

import com.setpik.server.analysis.domain.AnalysisStatus;
import com.setpik.server.analysis.domain.PlaylistAnalysis;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 기본 CRUD는 JpaRepository가 제공하고 도메인별 조회 메서드는 여기에 추가한다. */
public interface PlaylistAnalysisRepository extends JpaRepository<PlaylistAnalysis, Long> {

	Optional<PlaylistAnalysis> findByAnalysisIdAndUserId(Long analysisId, Long userId);
	Optional<PlaylistAnalysis> findFirstByPlaylistIdAndUserIdOrderByAnalyzedAtDescAnalysisIdDesc(
		Long playlistId, Long userId);
	/** FAILED/진행 중 분석을 최신 분석 기준으로 삼지 않도록 status를 함께 조건으로 둔다. */
	Optional<PlaylistAnalysis> findFirstByUserIdAndAnalysisStatusOrderByAnalyzedAtDescAnalysisIdDesc(
		Long userId, AnalysisStatus analysisStatus);

	boolean existsByAnalysisIdAndUserId(Long analysisId, Long userId);
}
