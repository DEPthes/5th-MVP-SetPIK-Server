package com.setpik.server.analysis.dto;

import com.setpik.server.analysis.domain.AnalysisStatus;
import java.util.List;

public record AnalysisDetailResponse(
	Long analysisId,
	AnalysisStatus analysisStatus,
	String warningMessage,
	Integer selectedArtistCount,
	List<AnalysisArtistResponse> topArtists
) {
}
