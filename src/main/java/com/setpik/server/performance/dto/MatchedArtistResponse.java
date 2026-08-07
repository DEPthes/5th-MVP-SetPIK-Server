package com.setpik.server.performance.dto;

import com.setpik.server.artist.domain.Artist;
import com.setpik.server.performance.domain.PerformanceMatchArtist;

public record MatchedArtistResponse(
	Long artistId,
	String artistName,
	Integer occurrenceCount,
	Boolean isHeadliner
) {
	public static MatchedArtistResponse of(PerformanceMatchArtist matchArtist, Artist artist, Boolean isHeadliner) {
		return new MatchedArtistResponse(
			artist.getArtistId(),
			artist.getArtistName(),
			matchArtist.getOccurrenceCount(),
			isHeadliner
		);
	}
}