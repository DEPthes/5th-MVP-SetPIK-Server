package com.setpik.server.playlist.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Spotify의 세부 장르를 KOPIS 공연 장르 체계로 변환한다. */
final class SpotifyGenreMapper {

	private static final List<String> POPULAR_MUSIC_KEYWORDS = List.of(
		"pop", "rock", "hip hop", "hip-hop", "rap", "r&b", "rnb", "soul",
		"indie", "metal", "punk", "electronic", "edm", "house", "techno", "jazz",
		"reggae", "folk", "blues", "singer-songwriter"
	);

	private SpotifyGenreMapper() {
	}

	static Set<String> toKopisGenres(List<String> spotifyGenres) {
		Set<String> result = new LinkedHashSet<>();
		if (spotifyGenres == null) {
			return result;
		}
		for (String genre : spotifyGenres) {
			if (genre == null || genre.isBlank()) {
				continue;
			}
			String normalized = genre.toLowerCase(Locale.ROOT);
			if (containsAny(normalized, "classical", "orchestra", "opera", "baroque")) {
				result.add("서양음악(클래식)");
			} else if (containsAny(normalized, "gugak", "traditional korean")) {
				result.add("한국음악(국악)");
			} else if (containsAny(normalized, "musical", "show tunes")) {
				result.add("뮤지컬");
			} else if (containsAny(normalized, "ballet", "dance")) {
				result.add("무용");
			} else if (POPULAR_MUSIC_KEYWORDS.stream().anyMatch(normalized::contains)) {
				result.add("대중음악");
			}
		}
		return result;
	}

	private static boolean containsAny(String value, String... keywords) {
		for (String keyword : keywords) {
			if (value.contains(keyword)) {
				return true;
			}
		}
		return false;
	}
}
