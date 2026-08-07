package com.setpik.server.playlist.dto;

import com.setpik.server.artist.domain.Artist;

public record TrackArtistResponse(
	Long artistId,
	String artistName
) {
	public static TrackArtistResponse from(Artist artist) {
		return new TrackArtistResponse(artist.getArtistId(), artist.getArtistName());
	}
}
