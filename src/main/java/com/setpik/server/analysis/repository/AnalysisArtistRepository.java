package com.setpik.server.analysis.repository;

import com.setpik.server.analysis.domain.AnalysisArtist;
import com.setpik.server.analysis.domain.AnalysisArtistId;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** 기본 CRUD는 JpaRepository가 제공하고 도메인별 조회 메서드는 여기에 추가한다. */
public interface AnalysisArtistRepository extends JpaRepository<AnalysisArtist, AnalysisArtistId> {

	List<AnalysisArtist> findByAnalysisIdAndIsExcludedFalse(Long analysisId);
	Page<AnalysisArtist> findByAnalysisId(Long analysisId, Pageable pageable);

	Page<AnalysisArtist> findByAnalysisIdAndIsExcludedFalse(Long analysisId, Pageable pageable);

	List<AnalysisArtist> findByAnalysisIdOrderByDisplayRankAsc(Long analysisId);

	void deleteByAnalysisId(Long analysisId);
}
