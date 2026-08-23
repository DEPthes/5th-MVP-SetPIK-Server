package com.setpik.server.playlist.client.dto;

/** Spotify 아티스트 검색의 정상적인 빈 결과와 API 호출 실패를 구분한다. */
public record SpotifyArtistSearchResult(
	SpotifyArtistSnapshot artist,
	boolean requestSucceeded
) {
	public static SpotifyArtistSearchResult success(SpotifyArtistSnapshot artist) {
		return new SpotifyArtistSearchResult(artist, true);
	}

	public static SpotifyArtistSearchResult failure() {
		return new SpotifyArtistSearchResult(null, false);
	}
}
