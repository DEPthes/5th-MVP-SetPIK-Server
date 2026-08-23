package com.setpik.server.analysis.dto;

import com.setpik.server.analysis.domain.AnalysisArtist;

/** 최신 분석 요약에서 노출하는 상위 아티스트 정보. */
public record TopArtistResponse(
	Long artistId,
	String artistName,
	String artistImageUrl,
	Integer occurrenceCount,
	Boolean isMajor,
	Boolean isExcluded
) {
	public static TopArtistResponse of(
		AnalysisArtist artist,
		String artistName,
		String artistImageUrl
	) {
		return new TopArtistResponse(
			artist.getArtistId(),
			artistName,
			artistImageUrl,
			artist.getOccurrenceCount(),
			artist.getIsMajor(),
			artist.getIsExcluded()
		);
	}
}
