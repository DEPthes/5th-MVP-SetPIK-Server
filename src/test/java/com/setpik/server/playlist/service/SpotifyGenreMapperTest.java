package com.setpik.server.playlist.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class SpotifyGenreMapperTest {

	@Test
	void mapsSpotifySubgenresToKopisGenres() {
		assertThat(SpotifyGenreMapper.toKopisGenres(List.of(
			"k-rap", "korean hip hop", "classical piano", "unknown"
		))).containsExactly("대중음악", "서양음악(클래식)");
	}

	@Test
	void ignoresMissingGenres() {
		assertThat(SpotifyGenreMapper.toKopisGenres(null)).isEmpty();
	}
}
