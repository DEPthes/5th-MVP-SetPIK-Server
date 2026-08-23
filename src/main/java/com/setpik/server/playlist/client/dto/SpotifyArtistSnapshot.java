package com.setpik.server.playlist.client.dto;

import java.util.List;

/** Spotify 트랙 응답에 포함된 아티스트 식별 정보다. */
public record SpotifyArtistSnapshot(
	String spotifyArtistId,
	String artistName,
	String spotifyArtistUrl,
	String imageUrl,
	Short popularity,
	List<String> genres
) {
	public SpotifyArtistSnapshot {
		genres = genres == null ? List.of() : List.copyOf(genres);
	}

	public SpotifyArtistSnapshot(
		String spotifyArtistId,
		String artistName,
		String spotifyArtistUrl,
		String imageUrl,
		Short popularity
	) {
		this(spotifyArtistId, artistName, spotifyArtistUrl, imageUrl, popularity, List.of());
	}

	public SpotifyArtistSnapshot(
		String spotifyArtistId,
		String artistName,
		String spotifyArtistUrl
	) {
		this(spotifyArtistId, artistName, spotifyArtistUrl, null, null, List.of());
	}
}
