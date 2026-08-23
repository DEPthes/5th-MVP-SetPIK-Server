package com.setpik.server.performance.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class PerformanceGenreCompatibilityPolicyTest {
	private final PerformanceGenreCompatibilityPolicy policy = new PerformanceGenreCompatibilityPolicy();

	@Test
	void allowsDetailedGenresOnlyForCompatibleKopisCategory() {
		assertThat(policy.isCompatible("K_POP", Set.of("대중음악"))).isTrue();
		assertThat(policy.isCompatible("K_POP", Set.of("연극"))).isFalse();
		assertThat(policy.isCompatible("CLASSICAL", Set.of("서양음악(클래식)"))).isTrue();
		assertThat(policy.isCompatible("CLASSICAL", Set.of("대중음악"))).isFalse();
	}

	@Test
	void missingArtistGenreStillRequiresMusicPerformance() {
		assertThat(policy.allowsDirectMatch(Set.of(), Set.of("대중음악"))).isTrue();
		assertThat(policy.allowsDirectMatch(Set.of(), Set.of("뮤지컬"))).isFalse();
		assertThat(policy.allowsDirectMatch(Set.of(), Set.of("연극"))).isFalse();
	}
}
