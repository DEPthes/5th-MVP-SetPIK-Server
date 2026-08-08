package com.setpik.server.performance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.setpik.server.analysis.domain.AnalysisArtist;
import com.setpik.server.analysis.domain.AnalysisStatus;
import com.setpik.server.analysis.domain.PlaylistAnalysis;
import com.setpik.server.analysis.repository.AnalysisArtistRepository;
import com.setpik.server.analysis.repository.PlaylistAnalysisRepository;
import com.setpik.server.artist.domain.Artist;
import com.setpik.server.artist.domain.ArtistGenre;
import com.setpik.server.artist.domain.Genre;
import com.setpik.server.artist.repository.ArtistGenreRepository;
import com.setpik.server.artist.repository.ArtistRepository;
import com.setpik.server.artist.repository.GenreRepository;
import com.setpik.server.common.exception.BusinessException;
import com.setpik.server.common.exception.ErrorCode;
import com.setpik.server.performance.domain.Performance;
import com.setpik.server.performance.domain.PerformanceArtist;
import com.setpik.server.performance.domain.PerformanceMatch;
import com.setpik.server.performance.domain.PerformanceMatchArtist;
import com.setpik.server.performance.domain.PerformanceGenre;
import com.setpik.server.performance.domain.PerformanceType;
import com.setpik.server.performance.domain.PerformanceTypeMap;
import com.setpik.server.performance.dto.PerformanceMatchRequest;
import com.setpik.server.performance.dto.PerformanceMatchResponse;
import com.setpik.server.performance.repository.PerformanceArtistRepository;
import com.setpik.server.performance.repository.PerformanceGenreRepository;
import com.setpik.server.performance.repository.PerformanceMatchArtistRepository;
import com.setpik.server.performance.repository.PerformanceMatchRepository;
import com.setpik.server.performance.repository.PerformanceRepository;
import com.setpik.server.performance.repository.PerformanceTypeMapRepository;
import com.setpik.server.performance.repository.PerformanceTypeRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PerformanceMatchingServiceTest {

	private final PlaylistAnalysisRepository playlistAnalysisRepository = mock(PlaylistAnalysisRepository.class);
	private final AnalysisArtistRepository analysisArtistRepository = mock(AnalysisArtistRepository.class);
	private final ArtistRepository artistRepository = mock(ArtistRepository.class);
	private final ArtistGenreRepository artistGenreRepository = mock(ArtistGenreRepository.class);
	private final GenreRepository genreRepository = mock(GenreRepository.class);
	private final PerformanceRepository performanceRepository = mock(PerformanceRepository.class);
	private final PerformanceArtistRepository performanceArtistRepository = mock(PerformanceArtistRepository.class);
	private final PerformanceGenreRepository performanceGenreRepository = mock(PerformanceGenreRepository.class);
	private final PerformanceTypeMapRepository performanceTypeMapRepository = mock(PerformanceTypeMapRepository.class);
	private final PerformanceTypeRepository performanceTypeRepository = mock(PerformanceTypeRepository.class);
	private final PerformanceMatchRepository performanceMatchRepository = mock(PerformanceMatchRepository.class);
	private final PerformanceMatchArtistRepository performanceMatchArtistRepository =
		mock(PerformanceMatchArtistRepository.class);

	private PerformanceMatchingService service;

	@BeforeEach
	void setUp() {
		service = new PerformanceMatchingService(
			playlistAnalysisRepository,
			analysisArtistRepository,
			artistRepository,
			artistGenreRepository,
			genreRepository,
			performanceRepository,
			performanceArtistRepository,
			performanceGenreRepository,
			performanceTypeMapRepository,
			performanceTypeRepository,
			performanceMatchRepository,
			performanceMatchArtistRepository
		);
	}

	@Test
	void createsFirstPriorityMatchUsingNormalizedArtistName() {
		PlaylistAnalysis analysis = mock(PlaylistAnalysis.class);
		when(analysis.getAnalysisId()).thenReturn(501L);
		when(analysis.getAnalysisStatus()).thenReturn(AnalysisStatus.COMPLETED);
		when(playlistAnalysisRepository.findByAnalysisIdAndUserId(501L, 1L))
			.thenReturn(Optional.of(analysis));

		AnalysisArtist selected = mock(AnalysisArtist.class);
		when(selected.getArtistId()).thenReturn(7L);
		when(selected.getOccurrenceCount()).thenReturn(3);
		when(selected.getIsMajor()).thenReturn(true);
		when(analysisArtistRepository.findByAnalysisIdAndIsExcludedFalse(501L))
			.thenReturn(List.of(selected));

		Performance performance = mock(Performance.class);
		when(performance.getPerformanceId()).thenReturn(10L);
		when(performance.getStartDate()).thenReturn(LocalDate.of(2026, 8, 20));
		when(performanceRepository.findMatchCandidates(any(), any())).thenReturn(List.of(performance));

		PerformanceArtist lineupArtist = mock(PerformanceArtist.class);
		when(lineupArtist.getPerformanceId()).thenReturn(10L);
		when(lineupArtist.getArtistId()).thenReturn(9L);
		when(performanceArtistRepository.findByPerformanceIdIn(List.of(10L)))
			.thenReturn(List.of(lineupArtist));

		Artist spotifyArtist = mock(Artist.class);
		when(spotifyArtist.getArtistId()).thenReturn(7L);
		when(spotifyArtist.getArtistName()).thenReturn("Artist A");
		Artist kopisArtist = mock(Artist.class);
		when(kopisArtist.getArtistId()).thenReturn(9L);
		when(kopisArtist.getArtistName()).thenReturn("Artist-A");
		when(artistRepository.findAllById(any())).thenReturn(List.of(spotifyArtist, kopisArtist));

		PerformanceTypeMap typeMap = mock(PerformanceTypeMap.class);
		when(typeMap.getPerformanceId()).thenReturn(10L);
		when(typeMap.getPerformanceTypeId()).thenReturn(100L);
		when(performanceTypeMapRepository.findByPerformanceIdIn(List.of(10L))).thenReturn(List.of(typeMap));
		PerformanceType soloType = mock(PerformanceType.class);
		when(soloType.getPerformanceTypeId()).thenReturn(100L);
		when(soloType.getTypeCode()).thenReturn("SOLO_CONCERT");
		when(soloType.getTypeName()).thenReturn("단독 콘서트");
		when(performanceTypeRepository.findAllById(List.of(100L))).thenReturn(List.of(soloType));
		when(performanceMatchRepository.findAllByAnalysisId(501L)).thenReturn(List.of());

		PerformanceMatch persistedMatch = mock(PerformanceMatch.class);
		when(persistedMatch.getMatchId()).thenReturn(700L);
		when(performanceMatchRepository.saveAndFlush(any(PerformanceMatch.class))).thenReturn(persistedMatch);

		PerformanceMatchResponse response = service.calculate(
			1L,
			501L,
			new PerformanceMatchRequest(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 12, 31))
		);

		assertThat(response.analysisId()).isEqualTo(501L);
		assertThat(response.matchedPerformanceCount()).isEqualTo(1);
		ArgumentCaptor<PerformanceMatch> matchCaptor = ArgumentCaptor.forClass(PerformanceMatch.class);
		verify(performanceMatchRepository).saveAndFlush(matchCaptor.capture());
		PerformanceMatch savedMatch = matchCaptor.getValue();
		assertThat(savedMatch.getMatchPriority()).isEqualTo((byte) 1);
		assertThat(savedMatch.getMatchedArtistCount()).isEqualTo(1);
		assertThat(savedMatch.getLineupArtistCount()).isEqualTo(1);
		assertThat(savedMatch.getMatchRatio()).isEqualTo((byte) 100);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<PerformanceMatchArtist>> artistCaptor = ArgumentCaptor.forClass(List.class);
		verify(performanceMatchArtistRepository).saveAll(artistCaptor.capture());
		assertThat(artistCaptor.getValue()).singleElement().satisfies(matchArtist -> {
			assertThat(matchArtist.getMatchId()).isEqualTo(700L);
			assertThat(matchArtist.getArtistId()).isEqualTo(9L);
			assertThat(matchArtist.getOccurrenceCount()).isEqualTo(3);
		});
	}

	@Test
	void rejectsReversedDateRange() {
		PerformanceMatchRequest request = new PerformanceMatchRequest(
			LocalDate.of(2026, 12, 31),
			LocalDate.of(2026, 8, 1)
		);

		assertThatThrownBy(() -> service.calculate(1L, 501L, request))
			.isInstanceOfSatisfying(BusinessException.class,
				exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
	}

	@Test
	void createsThirdPriorityMatchFromLineupArtistGenre() {
		PlaylistAnalysis analysis = mock(PlaylistAnalysis.class);
		when(analysis.getAnalysisId()).thenReturn(501L);
		when(analysis.getAnalysisStatus()).thenReturn(AnalysisStatus.COMPLETED);
		when(playlistAnalysisRepository.findByAnalysisIdAndUserId(501L, 1L))
			.thenReturn(Optional.of(analysis));

		AnalysisArtist selected = mock(AnalysisArtist.class);
		when(selected.getArtistId()).thenReturn(7L);
		when(selected.getOccurrenceCount()).thenReturn(2);
		when(selected.getIsMajor()).thenReturn(true);
		when(analysisArtistRepository.findByAnalysisIdAndIsExcludedFalse(501L))
			.thenReturn(List.of(selected));

		Performance performance = mock(Performance.class);
		when(performance.getPerformanceId()).thenReturn(10L);
		when(performance.getStartDate()).thenReturn(LocalDate.of(2026, 9, 1));
		when(performanceRepository.findMatchCandidates(any(), any())).thenReturn(List.of(performance));

		PerformanceArtist lineupArtist = mock(PerformanceArtist.class);
		when(lineupArtist.getPerformanceId()).thenReturn(10L);
		when(lineupArtist.getArtistId()).thenReturn(9L);
		when(performanceArtistRepository.findByPerformanceIdIn(List.of(10L)))
			.thenReturn(List.of(lineupArtist));

		Artist selectedArtist = mock(Artist.class);
		when(selectedArtist.getArtistId()).thenReturn(7L);
		when(selectedArtist.getArtistName()).thenReturn("Artist A");
		Artist differentLineupArtist = mock(Artist.class);
		when(differentLineupArtist.getArtistId()).thenReturn(9L);
		when(differentLineupArtist.getArtistName()).thenReturn("Artist B");
		when(artistRepository.findAllById(any()))
			.thenReturn(List.of(selectedArtist, differentLineupArtist));
		when(performanceTypeMapRepository.findByPerformanceIdIn(List.of(10L))).thenReturn(List.of());

		ArtistGenre preferredGenre = mock(ArtistGenre.class);
		when(preferredGenre.getArtistId()).thenReturn(7L);
		when(preferredGenre.getGenreId()).thenReturn(5L);
		ArtistGenre lineupGenre = mock(ArtistGenre.class);
		when(lineupGenre.getArtistId()).thenReturn(9L);
		when(lineupGenre.getGenreId()).thenReturn(5L);
		when(artistGenreRepository.findByArtistIdIn(List.of(7L))).thenReturn(List.of(preferredGenre));
		when(artistGenreRepository.findByArtistIdIn(List.of(9L))).thenReturn(List.of(lineupGenre));
		when(performanceGenreRepository.findByPerformanceIdIn(List.of(10L))).thenReturn(List.of());
		Genre genre = mock(Genre.class);
		when(genre.getGenreId()).thenReturn(5L);
		when(genre.getGenreName()).thenReturn("인디");
		when(genreRepository.findAllById(java.util.Set.of(5L))).thenReturn(List.of(genre));
		when(performanceMatchRepository.findAllByAnalysisId(501L)).thenReturn(List.of());

		PerformanceMatch persistedMatch = mock(PerformanceMatch.class);
		when(persistedMatch.getMatchId()).thenReturn(701L);
		when(performanceMatchRepository.saveAndFlush(any(PerformanceMatch.class))).thenReturn(persistedMatch);

		PerformanceMatchResponse response = service.calculate(
			1L, 501L, new PerformanceMatchRequest(null, null));

		assertThat(response.matchedPerformanceCount()).isEqualTo(1);
		ArgumentCaptor<PerformanceMatch> matchCaptor = ArgumentCaptor.forClass(PerformanceMatch.class);
		verify(performanceMatchRepository).saveAndFlush(matchCaptor.capture());
		PerformanceMatch savedMatch = matchCaptor.getValue();
		assertThat(savedMatch.getMatchPriority()).isEqualTo((byte) 3);
		assertThat(savedMatch.getMatchedArtistCount()).isZero();
		assertThat(savedMatch.getLineupArtistCount()).isEqualTo(1);
		assertThat(savedMatch.getMatchRatio()).isNull();
		assertThat(savedMatch.getGenreId()).isEqualTo(5L);
		assertThat(savedMatch.getRecommendationReason()).contains("인디");
	}

	@Test
	void hidesAnalysisOwnedByAnotherUser() {
		when(playlistAnalysisRepository.findByAnalysisIdAndUserId(501L, 2L))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.calculate(2L, 501L, new PerformanceMatchRequest(null, null)))
			.isInstanceOfSatisfying(BusinessException.class,
					exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
	}

	@Test
	void rejectsAnalysisWithoutSelectedArtists() {
		PlaylistAnalysis analysis = mock(PlaylistAnalysis.class);
		when(analysis.getAnalysisId()).thenReturn(501L);
		when(analysis.getAnalysisStatus()).thenReturn(AnalysisStatus.COMPLETED);
		when(playlistAnalysisRepository.findByAnalysisIdAndUserId(501L, 1L))
			.thenReturn(Optional.of(analysis));
		when(analysisArtistRepository.findByAnalysisIdAndIsExcludedFalse(501L)).thenReturn(List.of());

		assertThatThrownBy(() -> service.calculate(1L, 501L, new PerformanceMatchRequest(null, null)))
			.isInstanceOfSatisfying(BusinessException.class,
				exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
	}
}
