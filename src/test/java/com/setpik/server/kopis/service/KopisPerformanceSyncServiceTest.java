package com.setpik.server.kopis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.setpik.server.artist.domain.Artist;
import com.setpik.server.artist.domain.Genre;
import com.setpik.server.artist.repository.ArtistRepository;
import com.setpik.server.artist.repository.GenreRepository;
import com.setpik.server.kopis.client.KopisClient;
import com.setpik.server.kopis.dto.KopisPerformanceDetail;
import com.setpik.server.kopis.dto.KopisSyncResponse;
import com.setpik.server.kopis.dto.KopisVenueDetail;
import com.setpik.server.performance.domain.Performance;
import com.setpik.server.performance.domain.PerformanceArtist;
import com.setpik.server.performance.domain.PerformanceGenre;
import com.setpik.server.performance.domain.Venue;
import com.setpik.server.performance.repository.PerformanceArtistRepository;
import com.setpik.server.performance.repository.PerformanceGenreRepository;
import com.setpik.server.performance.repository.PerformanceRepository;
import com.setpik.server.performance.repository.VenueRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class KopisPerformanceSyncServiceTest {

	@Mock KopisClient kopisClient;
	@Mock VenueRepository venueRepository;
	@Mock PerformanceRepository performanceRepository;
	@Mock ArtistRepository artistRepository;
	@Mock GenreRepository genreRepository;
	@Mock PerformanceArtistRepository performanceArtistRepository;
	@Mock PerformanceGenreRepository performanceGenreRepository;
	@InjectMocks KopisPerformanceSyncService syncService;

	@Test
	void savesKopisDataUsingErdRelations() {
		LocalDate date = LocalDate.of(2026, 8, 15);
		KopisPerformanceDetail detail = new KopisPerformanceDetail(
			"PF001", "SetPIK Festival", date, date.plusDays(2),
			"https://example.com/poster.jpg", "https://tickets.example.com/1",
			"공연예정", "전석 100,000원", "인천광역시", "대중음악",
			"FC001", "송도달빛축제공원", List.of("Artist A"));
		KopisVenueDetail venueDetail = new KopisVenueDetail(
			"FC001", "송도달빛축제공원", "인천광역시 연수구 센트럴로 350",
			new BigDecimal("37.3921"), new BigDecimal("126.6399"));

		when(kopisClient.getPerformanceIds(date, date, 1, 100)).thenReturn(List.of("PF001"));
		when(kopisClient.getPerformanceDetail("PF001")).thenReturn(detail);
		when(kopisClient.getVenueDetail("FC001")).thenReturn(venueDetail);
		when(performanceRepository.findByKopisPerformanceId("PF001"))
			.thenReturn(Optional.empty(), Optional.empty());
		when(venueRepository.findByKopisVenueId("FC001")).thenReturn(Optional.empty());
		when(venueRepository.save(any(Venue.class))).thenAnswer(invocation -> {
			Venue venue = invocation.getArgument(0);
			ReflectionTestUtils.setField(venue, "venueId", 77L);
			return venue;
		});
		when(performanceRepository.save(any(Performance.class))).thenAnswer(invocation -> {
			Performance performance = invocation.getArgument(0);
			ReflectionTestUtils.setField(performance, "performanceId", 1001L);
			return performance;
		});
		when(artistRepository.findByNormalizedName("artist a")).thenReturn(Optional.empty());
		when(artistRepository.save(any(Artist.class))).thenAnswer(invocation -> {
			Artist artist = invocation.getArgument(0);
			ReflectionTestUtils.setField(artist, "artistId", 7L);
			return artist;
		});
		when(genreRepository.findByNormalizedName("대중음악")).thenReturn(Optional.empty());
		when(genreRepository.save(any(Genre.class))).thenAnswer(invocation -> {
			Genre genre = invocation.getArgument(0);
			ReflectionTestUtils.setField(genre, "genreId", 3L);
			return genre;
		});

		KopisSyncResponse response = syncService.sync(date, date);

		assertThat(response.createdPerformanceCount()).isEqualTo(1);
		assertThat(response.updatedPerformanceCount()).isZero();

		ArgumentCaptor<Performance> performanceCaptor = ArgumentCaptor.forClass(Performance.class);
		verify(performanceRepository).save(performanceCaptor.capture());
		assertThat(performanceCaptor.getValue().getKopisPerformanceId()).isEqualTo("PF001");
		assertThat(performanceCaptor.getValue().getVenueId()).isEqualTo(77L);

		ArgumentCaptor<PerformanceArtist> artistMapCaptor = ArgumentCaptor.forClass(PerformanceArtist.class);
		verify(performanceArtistRepository).save(artistMapCaptor.capture());
		assertThat(artistMapCaptor.getValue().getPerformanceId()).isEqualTo(1001L);
		assertThat(artistMapCaptor.getValue().getArtistId()).isEqualTo(7L);
		assertThat(artistMapCaptor.getValue().getIsHeadliner()).isFalse();

		ArgumentCaptor<PerformanceGenre> genreMapCaptor = ArgumentCaptor.forClass(PerformanceGenre.class);
		verify(performanceGenreRepository).save(genreMapCaptor.capture());
		assertThat(genreMapCaptor.getValue().getGenreId()).isEqualTo(3L);
		assertThat(genreMapCaptor.getValue().getSourceType()).isEqualTo("KOPIS");
	}
}
