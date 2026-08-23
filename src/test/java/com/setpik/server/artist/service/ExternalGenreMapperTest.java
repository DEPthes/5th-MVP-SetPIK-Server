package com.setpik.server.artist.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class ExternalGenreMapperTest {
	@Test
	void keepsPopularMusicSubgenresSeparate() {
		assertThat(ExternalGenreMapper.toCanonicalGenres(List.of(
			"K-pop", "Korean hip hop", "contemporary R&B", "indie rock"
		))).containsExactly("K_POP", "HIP_HOP_RAP", "RNB_SOUL", "INDIE", "ROCK");
	}

	@Test
	void doesNotTreatDancePopAsDancePerformance() {
		assertThat(ExternalGenreMapper.toCanonicalGenres(List.of("dance-pop")))
			.containsExactly("POP");
		assertThat(ExternalGenreMapper.toCanonicalGenres(List.of("contemporary dance")))
			.containsExactly("DANCE_PERFORMANCE");
	}
}
