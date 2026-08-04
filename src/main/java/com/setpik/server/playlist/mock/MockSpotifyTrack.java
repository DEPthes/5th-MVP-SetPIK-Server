package com.setpik.server.playlist.mock;

public record MockSpotifyTrack(
	String spotifyTrackId,
	String trackName,
	String albumName,
	String albumImageUrl,
	String spotifyTrackUrl,
	String previewUrl,
	Integer durationMs,
	Boolean isPlayable
) {
}
