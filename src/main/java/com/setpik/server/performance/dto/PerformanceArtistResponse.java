package com.setpik.server.performance.dto;

import com.setpik.server.artist.domain.Artist;
import com.setpik.server.performance.domain.PerformanceArtist;

public record PerformanceArtistResponse(
	Long artistId,
	String artistName,
	String imageUrl,
	String spotifyArtistUrl,
	Boolean isHeadliner,
	Long lineupOrder
) {
	public static PerformanceArtistResponse of(PerformanceArtist performanceArtist, Artist artist) {
		return new PerformanceArtistResponse(
			artist.getArtistId(),
			artist.getArtistName(),
			artist.getImageUrl(),
			artist.getSpotifyArtistUrl(),
			performanceArtist.getIsHeadliner(),
			performanceArtist.getLineupOrder()
		);
	}
}