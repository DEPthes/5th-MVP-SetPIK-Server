package com.setpik.server.analysis.dto;

import com.setpik.server.analysis.domain.AnalysisStatus;
import com.setpik.server.analysis.domain.PlaylistAnalysis;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public record AnalysisResponse(
	Long analysisId,
	AnalysisStatus analysisStatus,
	Integer totalTrackCount,
	Integer selectedArtistCount,
	OffsetDateTime analyzedAt
) {
	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	public static AnalysisResponse from(PlaylistAnalysis analysis) {
		return new AnalysisResponse(
			analysis.getAnalysisId(),
			analysis.getAnalysisStatus(),
			analysis.getTotalTrackCount(),
			analysis.getSelectedArtistCount(),
			analysis.getAnalyzedAt().atZone(KST).toOffsetDateTime()
		);
	}
}
