package com.setpik.server.analysis.dto;

import com.setpik.server.analysis.domain.AnalysisArtist;

public record AnalysisArtistResponse(
	Long artistId,
	String artistName,
	String artistImageUrl,
	Integer occurrenceCount,
	Short popularitySnapshot,
	Boolean isMajor,
	Boolean isExcluded,
	Integer displayRank
) {
	public static AnalysisArtistResponse of(
		AnalysisArtist analysisArtist,
		String artistName,
		String artistImageUrl
	) {
		return new AnalysisArtistResponse(
			analysisArtist.getArtistId(),
			artistName,
			artistImageUrl,
			analysisArtist.getOccurrenceCount(),
			analysisArtist.getPopularitySnapshot(),
			analysisArtist.getIsMajor(),
			analysisArtist.getIsExcluded(),
			analysisArtist.getDisplayRank()
		);
	}
}
