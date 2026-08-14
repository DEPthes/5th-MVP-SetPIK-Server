package com.setpik.server.analysis.dto;

import com.setpik.server.analysis.domain.AnalysisArtist;

/** 최신 분석 요약에서 노출하는 상위 아티스트 정보. */
public record TopArtistResponse(
	Long artistId,
	String artistName,
	Integer occurrenceCount,
	Boolean isMajor,
	Boolean isExcluded
) {
	public static TopArtistResponse of(AnalysisArtist artist, String artistName) {
		return new TopArtistResponse(
			artist.getArtistId(),
			artistName,
			artist.getOccurrenceCount(),
			artist.getIsMajor(),
			artist.getIsExcluded()
		);
	}
}
