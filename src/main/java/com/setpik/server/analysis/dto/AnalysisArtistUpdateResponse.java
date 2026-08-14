package com.setpik.server.analysis.dto;

public record AnalysisArtistUpdateResponse(
	Long analysisId,
	Integer updatedArtistCount
) {
}
