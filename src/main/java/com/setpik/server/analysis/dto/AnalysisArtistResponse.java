package com.setpik.server.analysis.dto;

import com.setpik.server.analysis.domain.AnalysisArtist;

public record AnalysisArtistResponse(
	Long artistId,
	String artistName,
	Integer occurrenceCount,
	Short popularitySnapshot,
	Boolean isMajor,
	Boolean isExcluded,
	Integer displayRank
) {
	public static AnalysisArtistResponse of(AnalysisArtist analysisArtist, String artistName) {
		return new AnalysisArtistResponse(
			analysisArtist.getArtistId(),
			artistName,
			analysisArtist.getOccurrenceCount(),
			analysisArtist.getPopularitySnapshot(),
			analysisArtist.getIsMajor(),
			analysisArtist.getIsExcluded(),
			analysisArtist.getDisplayRank()
		);
	}
}
