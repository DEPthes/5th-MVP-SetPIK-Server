package com.setpik.server.playlist.service;

import java.util.List;
import java.util.Set;
import com.setpik.server.artist.service.ExternalGenreMapper;

/** Spotify의 세부 장르를 추천용 공통 세부 장르 코드로 변환한다. */
final class SpotifyGenreMapper {
	private SpotifyGenreMapper() {
	}

	static Set<String> toKopisGenres(List<String> spotifyGenres) {
		return ExternalGenreMapper.toCanonicalGenres(spotifyGenres);
	}
}
