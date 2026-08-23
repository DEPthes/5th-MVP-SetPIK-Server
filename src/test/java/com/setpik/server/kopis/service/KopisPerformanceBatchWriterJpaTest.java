package com.setpik.server.kopis.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.setpik.server.artist.domain.Artist;
import com.setpik.server.artist.repository.ArtistRepository;
import com.setpik.server.artist.repository.GenreRepository;
import com.setpik.server.common.config.JpaConfig;
import com.setpik.server.kopis.dto.KopisPerformanceDetail;
import com.setpik.server.kopis.dto.KopisVenueDetail;
import com.setpik.server.performance.domain.Performance;
import com.setpik.server.performance.domain.PerformanceArtist;
import com.setpik.server.performance.domain.PerformanceStatus;
import com.setpik.server.performance.domain.Venue;
import com.setpik.server.performance.repository.PerformanceArtistRepository;
import com.setpik.server.performance.repository.PerformanceGenreRepository;
import com.setpik.server.performance.repository.PerformanceRepository;
import com.setpik.server.performance.repository.PerformanceTypeMapRepository;
import com.setpik.server.performance.repository.PerformanceTypeRepository;
import com.setpik.server.performance.repository.VenueRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
class KopisPerformanceBatchWriterJpaTest {

	@Autowired private VenueRepository venueRepository;
	@Autowired private PerformanceRepository performanceRepository;
	@Autowired private ArtistRepository artistRepository;
	@Autowired private GenreRepository genreRepository;
	@Autowired private PerformanceArtistRepository performanceArtistRepository;
	@Autowired private PerformanceGenreRepository performanceGenreRepository;
	@Autowired private PerformanceTypeRepository performanceTypeRepository;
	@Autowired private PerformanceTypeMapRepository performanceTypeMapRepository;

	@Test
	void resyncReplacesExistingSpotifyArtistMappingWithKopisSourceArtist() {
		Venue venue = venueRepository.saveAndFlush(new Venue(
			"FC-RESTORE", "원본 공연장", "서울", "중구", "서울 중구", null, null));
		LocalDate date = LocalDate.of(2026, 8, 23);
		Performance performance = performanceRepository.saveAndFlush(new Performance(
			"PF-RESTORE", "원본 출연진 복구 공연", date, date.plusDays(1),
			null, null, PerformanceStatus.SCHEDULED, "PAID", "10,000원",
			null, null, LocalDateTime.now(), venue.getVenueId()));
		Artist incorrectlyLinkedSpotifyArtist = artistRepository.saveAndFlush(
			new Artist("spotify-hanroro", "HANRORO", null));
		Artist kopisSourceArtist = artistRepository.saveAndFlush(Artist.fromKopis("에토레 파가노"));
		performanceArtistRepository.saveAndFlush(new PerformanceArtist(
			incorrectlyLinkedSpotifyArtist.getArtistId(), performance.getPerformanceId(), 1L, false));

		writer().writeBatch(
			List.of(new KopisPerformanceDetail(
				"PF-RESTORE", "원본 출연진 복구 공연", date, date.plusDays(1),
				null, null, "공연예정", "10,000원", "서울", "서양음악(클래식)",
				"FC-RESTORE", "원본 공연장", List.of("에토레 파가노"))),
			Map.of("FC-RESTORE", new KopisVenueDetail(
				"FC-RESTORE", "원본 공연장", "서울 중구", BigDecimal.ONE, BigDecimal.ONE)),
			LocalDateTime.now());

		List<PerformanceArtist> mappings = performanceArtistRepository
			.findByPerformanceIdOrderByLineupOrderAsc(performance.getPerformanceId());
		assertThat(mappings)
			.extracting(PerformanceArtist::getArtistId)
			.containsExactly(kopisSourceArtist.getArtistId());
		assertThat(mappings)
			.extracting(PerformanceArtist::getArtistId)
			.doesNotContain(incorrectlyLinkedSpotifyArtist.getArtistId());
	}

	private KopisPerformanceBatchWriter writer() {
		return new KopisPerformanceBatchWriter(
			venueRepository,
			performanceRepository,
			artistRepository,
			genreRepository,
			performanceArtistRepository,
			performanceGenreRepository,
			performanceTypeRepository,
			performanceTypeMapRepository
		);
	}
}
