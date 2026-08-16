package com.setpik.server.playlist.client.dto;

/** Spotify 트랙 응답에 포함된 아티스트 식별 정보다. */
public record SpotifyArtistSnapshot(
	String spotifyArtistId,
	String artistName,
	String spotifyArtistUrl,
	String imageUrl,
	Short popularity
) {
	public SpotifyArtistSnapshot(
		String spotifyArtistId,
		String artistName,
		String spotifyArtistUrl
	) {
		this(spotifyArtistId, artistName, spotifyArtistUrl, null, null);
	}
}
