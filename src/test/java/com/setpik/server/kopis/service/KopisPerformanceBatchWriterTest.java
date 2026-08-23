package com.setpik.server.kopis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.setpik.server.artist.domain.Artist;
import com.setpik.server.artist.domain.Genre;
import com.setpik.server.artist.repository.ArtistRepository;
import com.setpik.server.artist.repository.GenreRepository;
import com.setpik.server.kopis.dto.KopisPerformanceDetail;
import com.setpik.server.kopis.dto.KopisVenueDetail;
import com.setpik.server.performance.domain.Performance;
import com.setpik.server.performance.domain.Venue;
import com.setpik.server.performance.repository.PerformanceArtistRepository;
import com.setpik.server.performance.repository.PerformanceGenreRepository;
import com.setpik.server.performance.repository.PerformanceRepository;
import com.setpik.server.performance.repository.PerformanceTypeMapRepository;
import com.setpik.server.performance.repository.PerformanceTypeRepository;
import com.setpik.server.performance.repository.VenueRepository;
import com.setpik.server.playlist.client.dto.SpotifyArtistSnapshot;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class KopisPerformanceBatchWriterTest {

	@Mock private VenueRepository venueRepository;
	@Mock private PerformanceRepository performanceRepository;
	@Mock private ArtistRepository artistRepository;
	@Mock private GenreRepository genreRepository;
	@Mock private PerformanceArtistRepository performanceArtistRepository;
	@Mock private PerformanceGenreRepository performanceGenreRepository;
	@Mock private PerformanceTypeRepository performanceTypeRepository;
	@Mock private PerformanceTypeMapRepository performanceTypeMapRepository;
	@Mock private com.setpik.server.auth.client.SpotifyOAuthClient spotifyOAuthClient;
	@Mock private com.setpik.server.playlist.client.SpotifyPlaylistClient spotifyPlaylistClient;

	private KopisPerformanceBatchWriter writer;

	@BeforeEach
	void setUp() {
		writer = new KopisPerformanceBatchWriter(
			venueRepository,
			performanceRepository,
			artistRepository,
			genreRepository,
			performanceArtistRepository,
			performanceGenreRepository,
			performanceTypeRepository,
			performanceTypeMapRepository,
			spotifyOAuthClient,
			spotifyPlaylistClient
		);
	}

	@Test
	void savesErdRelationsUsingBulkRepositoryOperations() {
		KopisPerformanceDetail detail = detail();
		KopisVenueDetail venueDetail = venue();
		when(spotifyOAuthClient.getClientCredentialsToken()).thenReturn("test-token");
		when(spotifyPlaylistClient.searchArtistByName(any(), any())).thenReturn(null);
		when(venueRepository.findByKopisVenueIdIn(List.of("FC001"))).thenReturn(List.of());
		when(venueRepository.saveAllAndFlush(any())).thenAnswer(invocation -> {
			Collection<Venue> venues = invocation.getArgument(0);
			venues.forEach(venue -> ReflectionTestUtils.setField(venue, "venueId", 77L));
			return List.copyOf(venues);
		});
		when(performanceRepository.findByKopisPerformanceIdIn(List.of("PF001"))).thenReturn(List.of());
		when(performanceRepository.saveAllAndFlush(anyList())).thenAnswer(invocation -> {
			List<Performance> performances = invocation.getArgument(0);
			performances.forEach(performance ->
				ReflectionTestUtils.setField(performance, "performanceId", 1001L));
			return performances;
		});
		when(artistRepository.findByNormalizedNameIn(List.of("artist a"))).thenReturn(List.of());
		when(artistRepository.saveAllAndFlush(anyList())).thenAnswer(invocation -> {
			List<Artist> artists = invocation.getArgument(0);
			artists.forEach(artist -> ReflectionTestUtils.setField(artist, "artistId", 7L));
			return artists;
		});
		when(genreRepository.findByNormalizedNameIn(List.of("대중음악"))).thenReturn(List.of());
		when(genreRepository.saveAllAndFlush(anyList())).thenAnswer(invocation -> {
			List<Genre> genres = invocation.getArgument(0);
			genres.forEach(genre -> ReflectionTestUtils.setField(genre, "genreId", 3L));
			return genres;
		});
		when(performanceTypeRepository.findByTypeCodeIn(anyList())).thenReturn(List.of());
		when(performanceTypeRepository.saveAllAndFlush(anyList())).thenAnswer(invocation -> {
			List<com.setpik.server.performance.domain.PerformanceType> types = invocation.getArgument(0);
			long id = 1L;
			for (var type : types) ReflectionTestUtils.setField(type, "performanceTypeId", id++);
			return types;
		});

		KopisBatchWriteResult result = writer.writeBatch(
			List.of(detail), Map.of("FC001", venueDetail), LocalDateTime.now());

		assertThat(result.createdCount()).isEqualTo(1);
		assertThat(result.updatedCount()).isZero();
		verify(performanceArtistRepository).deleteByPerformanceIdIn(List.of(1001L));
		verify(performanceGenreRepository).deleteByPerformanceIdIn(List.of(1001L));
		verify(performanceArtistRepository).saveAll(anyList());
		verify(performanceGenreRepository).saveAll(anyList());
		verify(performanceTypeMapRepository).saveAll(anyList());
	}

	@Test
	void matchesExistingSpotifyArtistOnlyForMusicGenre() {
		KopisPerformanceDetail detail = detailWith("PF002", "대중음악", "백예린");
		stubCommonRepositories(detail);

		Artist existingSpotifyArtist = new Artist("SPOT1", "Yerin Baek", null);
		ReflectionTestUtils.setField(existingSpotifyArtist, "artistId", 99L);

		when(spotifyOAuthClient.getClientCredentialsToken()).thenReturn("test-token");
		when(artistRepository.findByNormalizedNameIn(List.of("백예린"))).thenReturn(List.of());
		when(spotifyPlaylistClient.searchArtistByName("test-token", "백예린"))
			.thenReturn(new SpotifyArtistSnapshot("SPOT1", "Yerin Baek", null));
		when(artistRepository.findBySpotifyArtistId("SPOT1")).thenReturn(Optional.of(existingSpotifyArtist));

		writer.writeBatch(List.of(detail), Map.of("FC001", venue()), LocalDateTime.now());

		assertThat(existingSpotifyArtist.getKopisArtistId()).isEqualTo("백예린");
	}

	/** 연극 등 비음악 장르는 배우 이름까지 Spotify 뮤지션 검색에 태워 엉뚱한 아티스트와 연결되는 것을 막는다. */
	@Test
	void skipsSpotifyMatchingForNonMusicGenre() {
		KopisPerformanceDetail detail = detailWith("PF003", "연극", "이유리");
		stubCommonRepositories(detail);

		when(artistRepository.findByNormalizedNameIn(List.of("이유리"))).thenReturn(List.of());
		when(artistRepository.saveAllAndFlush(anyList())).thenAnswer(invocation -> {
			List<Artist> artists = invocation.getArgument(0);
			artists.forEach(artist -> ReflectionTestUtils.setField(artist, "artistId", 55L));
			return artists;
		});

		writer.writeBatch(List.of(detail), Map.of("FC001", venue()), LocalDateTime.now());

		verify(spotifyOAuthClient, never()).getClientCredentialsToken();
		verify(spotifyPlaylistClient, never()).searchArtistByName(any(), any());
	}

	private void stubCommonRepositories(KopisPerformanceDetail detail) {
		when(venueRepository.findByKopisVenueIdIn(List.of(detail.facilityId()))).thenReturn(List.of());
		when(venueRepository.saveAllAndFlush(any())).thenAnswer(invocation -> {
			Collection<Venue> venues = invocation.getArgument(0);
			venues.forEach(venue -> ReflectionTestUtils.setField(venue, "venueId", 77L));
			return List.copyOf(venues);
		});
		when(performanceRepository.findByKopisPerformanceIdIn(List.of(detail.kopisPerformanceId())))
			.thenReturn(List.of());
		when(performanceRepository.saveAllAndFlush(anyList())).thenAnswer(invocation -> {
			List<Performance> performances = invocation.getArgument(0);
			performances.forEach(performance ->
				ReflectionTestUtils.setField(performance, "performanceId", 1001L));
			return performances;
		});
		when(genreRepository.findByNormalizedNameIn(List.of(Artist.normalize(detail.genreName()))))
			.thenReturn(List.of());
		when(genreRepository.saveAllAndFlush(anyList())).thenAnswer(invocation -> {
			List<Genre> genres = invocation.getArgument(0);
			genres.forEach(genre -> ReflectionTestUtils.setField(genre, "genreId", 3L));
			return genres;
		});
		when(performanceTypeRepository.findByTypeCodeIn(anyList())).thenReturn(List.of());
		when(performanceTypeRepository.saveAllAndFlush(anyList())).thenAnswer(invocation -> {
			List<com.setpik.server.performance.domain.PerformanceType> types = invocation.getArgument(0);
			long id = 1L;
			for (var type : types) ReflectionTestUtils.setField(type, "performanceTypeId", id++);
			return types;
		});
	}

	private KopisPerformanceDetail detailWith(String kopisPerformanceId, String genreName, String artistName) {
		LocalDate date = LocalDate.of(2026, 8, 15);
		return new KopisPerformanceDetail(
			kopisPerformanceId, "Test Performance", date, date.plusDays(2),
			"https://example.com/poster.jpg", "https://tickets.example.com/1",
			"공연예정", "전석 100,000원", "인천광역시", genreName,
			"FC001", "송도달빛축제공원", List.of(artistName));
	}

	private KopisPerformanceDetail detail() {
		LocalDate date = LocalDate.of(2026, 8, 15);
		return new KopisPerformanceDetail(
			"PF001", "SetPIK Festival", date, date.plusDays(2),
			"https://example.com/poster.jpg", "https://tickets.example.com/1",
			"공연예정", "전석 100,000원", "인천광역시", "대중음악",
			"FC001", "송도달빛축제공원", List.of("Artist A"));
	}

	private KopisVenueDetail venue() {
		return new KopisVenueDetail(
			"FC001", "송도달빛축제공원", "인천광역시 연수구 센트럴로 350",
			new BigDecimal("37.3921"), new BigDecimal("126.6399"));
	}
}
