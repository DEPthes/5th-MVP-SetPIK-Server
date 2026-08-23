package com.setpik.server.playlist.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class SpotifyGenreMapperTest {

	@Test
	void mapsSpotifySubgenresToDetailedCanonicalGenres() {
		assertThat(SpotifyGenreMapper.toKopisGenres(List.of(
			"korean hip hop", "classical piano", "unknown"
		))).containsExactly("HIP_HOP_RAP", "CLASSICAL");
	}

	@Test
	void ignoresMissingGenres() {
		assertThat(SpotifyGenreMapper.toKopisGenres(null)).isEmpty();
	}
}
