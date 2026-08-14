package com.setpik.server.analysis.dto;

import com.setpik.server.analysis.domain.AnalysisStatus;
import com.setpik.server.analysis.domain.PlaylistAnalysis;
import java.time.LocalDateTime;

public record AnalysisResponse(
	Long analysisId,
	AnalysisStatus analysisStatus,
	Integer totalTrackCount,
	Integer selectedArtistCount,
	LocalDateTime analyzedAt
) {
	public static AnalysisResponse from(PlaylistAnalysis analysis) {
		return new AnalysisResponse(
			analysis.getAnalysisId(),
			analysis.getAnalysisStatus(),
			analysis.getTotalTrackCount(),
			analysis.getSelectedArtistCount(),
			analysis.getAnalyzedAt()
		);
	}
}
