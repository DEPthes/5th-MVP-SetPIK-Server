package com.setpik.server.performance.service;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/** 외부 세부 장르가 현재 KOPIS 공연 대분류에서 유효한지 판단한다. */
final class PerformanceGenreCompatibilityPolicy {

	private static final Set<String> POPULAR_MUSIC = Set.of(
		"K_POP", "POP", "HIP_HOP_RAP", "RNB_SOUL", "ROCK", "INDIE",
		"ELECTRONIC", "JAZZ", "BALLAD", "FOLK", "METAL_PUNK"
	);

	boolean allowsDirectMatch(Set<String> artistGenres, Set<String> performanceGenres) {
		if (performanceGenres.isEmpty()) return false;
		if (artistGenres.isEmpty()) {
			return performanceGenres.stream().anyMatch(this::isMusicPerformanceGenre);
		}
		return artistGenres.stream().anyMatch(genre -> isCompatible(genre, performanceGenres));
	}

	boolean isCompatible(String detailedGenre, Set<String> performanceGenres) {
		if (POPULAR_MUSIC.contains(detailedGenre)) return contains(performanceGenres, "대중음악");
		return switch (detailedGenre) {
			case "CLASSICAL" -> contains(performanceGenres, "서양음악(클래식)");
			case "KOREAN_TRADITIONAL" -> contains(performanceGenres, "한국음악(국악)");
			case "MUSICAL" -> contains(performanceGenres, "뮤지컬");
			case "DANCE_PERFORMANCE" -> contains(performanceGenres, "무용(서양/한국무용)")
				|| contains(performanceGenres, "대중무용");
			default -> false;
		};
	}

	Set<String> specificGenresFromKopis(Set<String> performanceGenres) {
		Set<String> result = new LinkedHashSet<>();
		if (contains(performanceGenres, "서양음악(클래식)")) result.add("CLASSICAL");
		if (contains(performanceGenres, "한국음악(국악)")) result.add("KOREAN_TRADITIONAL");
		if (contains(performanceGenres, "뮤지컬")) result.add("MUSICAL");
		if (contains(performanceGenres, "무용(서양/한국무용)") || contains(performanceGenres, "대중무용")) {
			result.add("DANCE_PERFORMANCE");
		}
		return result;
	}

	private boolean isMusicPerformanceGenre(String genre) {
		String normalized = genre.toLowerCase(Locale.ROOT);
		return normalized.contains("음악") || normalized.contains("클래식") || normalized.contains("국악");
	}

	private boolean contains(Set<String> genres, String expected) {
		return genres.stream().anyMatch(expected::equalsIgnoreCase);
	}
}
