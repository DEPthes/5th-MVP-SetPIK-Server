package com.setpik.server.performance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.setpik.server.analysis.repository.PlaylistAnalysisRepository;
import com.setpik.server.artist.domain.ArtistAlias;
import com.setpik.server.artist.repository.ArtistAliasRepository;
import com.setpik.server.artist.repository.ArtistRepository;
import com.setpik.server.common.api.PageResponse;
import com.setpik.server.common.exception.BusinessException;
import com.setpik.server.common.exception.ErrorCode;
import com.setpik.server.performance.domain.Performance;
import com.setpik.server.performance.domain.PerformanceMatch;
import com.setpik.server.performance.domain.PerformanceMatchArtist;
import com.setpik.server.performance.domain.PerformanceArtist;
import com.setpik.server.performance.domain.PerformanceStatus;
import com.setpik.server.performance.domain.SaleStatus;
import com.setpik.server.performance.domain.TicketSaleType;
import com.setpik.server.performance.domain.TicketSchedule;
import com.setpik.server.performance.domain.Venue;
import com.setpik.server.performance.dto.PerformanceDetailResponse;
import com.setpik.server.performance.dto.MatchedArtistResponse;
import com.setpik.server.performance.dto.TicketScheduleResponse;
import com.setpik.server.performance.dto.PerformanceRecommendationResponse;
import com.setpik.server.performance.repository.PerformanceArtistRepository;
import com.setpik.server.performance.repository.PerformanceMatchArtistRepository;
import com.setpik.server.performance.repository.PerformanceMatchRepository;
import com.setpik.server.performance.repository.PerformanceRepository;
import com.setpik.server.performance.repository.TicketScheduleRepository;
import com.setpik.server.performance.repository.VenueRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

class PerformanceServiceTest {

	private final PerformanceRepository performanceRepository = mock(PerformanceRepository.class);
	private final VenueRepository venueRepository = mock(VenueRepository.class);
	private final TicketScheduleRepository ticketScheduleRepository = mock(TicketScheduleRepository.class);
	private final PerformanceArtistRepository performanceArtistRepository = mock(PerformanceArtistRepository.class);
	private final ArtistRepository artistRepository = mock(ArtistRepository.class);
	private final ArtistAliasRepository artistAliasRepository = mock(ArtistAliasRepository.class);
	private final PerformanceMatchRepository performanceMatchRepository = mock(PerformanceMatchRepository.class);
	private final PerformanceMatchArtistRepository performanceMatchArtistRepository =
		mock(PerformanceMatchArtistRepository.class);
	private final PlaylistAnalysisRepository playlistAnalysisRepository = mock(PlaylistAnalysisRepository.class);
	private final PerformanceMetadataLookupService performanceMetadataLookupService =
		mock(PerformanceMetadataLookupService.class);

	private PerformanceService service;

	@BeforeEach
	void setUp() {
		service = new PerformanceService(
			performanceRepository,
			venueRepository,
			ticketScheduleRepository,
			performanceArtistRepository,
			artistRepository,
			artistAliasRepository,
			performanceMatchRepository,
			performanceMatchArtistRepository,
			playlistAnalysisRepository,
			performanceMetadataLookupService
		);
	}

	@Test
	void returnsOwnedAnalysisRecommendationsWithRequestedSortAndHasNext() {
		when(playlistAnalysisRepository.existsByAnalysisIdAndUserId(501L, 1L)).thenReturn(true);
		PerformanceMatch match = mock(PerformanceMatch.class);
		when(match.getMatchId()).thenReturn(900L);
		when(match.getPerformanceId()).thenReturn(1001L);
		when(match.getMatchPriority()).thenReturn((byte) 1);
		when(match.getMatchedArtistCount()).thenReturn(2);
		when(match.getMatchRatio()).thenReturn((byte) 40);
		when(match.getRecommendationReason()).thenReturn("추천 이유");
		when(performanceMatchRepository.findVisibleByAnalysisId(any(), any()))
			.thenReturn(new PageImpl<>(List.of(match), PageRequest.of(0, 1), 2));

		Performance performance = mock(Performance.class);
		when(performance.getPerformanceId()).thenReturn(1001L);
		when(performance.getPerformanceName()).thenReturn("공연명");
		when(performance.getVenueId()).thenReturn(77L);
		when(performance.getPosterUrl()).thenReturn("https://images.example.com/performances/1001.jpg");
		when(performance.getStartDate()).thenReturn(java.time.LocalDate.of(2026, 8, 15));
		when(performance.getEndDate()).thenReturn(java.time.LocalDate.of(2026, 8, 17));
		when(performance.getPerformanceStatus()).thenReturn(PerformanceStatus.ON_SALE);
		when(performance.getMinTicketPrice()).thenReturn(80000);
		when(performanceRepository.findAllById(List.of(1001L))).thenReturn(List.of(performance));

		Venue venue = mock(Venue.class);
		when(venue.getVenueId()).thenReturn(77L);
		when(venue.getVenueName()).thenReturn("송도달빛축제공원");
		when(venue.getCity()).thenReturn("인천");
		when(venueRepository.findAllById(List.of(77L))).thenReturn(List.of(venue));

		when(performanceMetadataLookupService.performanceTypeCodeByPerformanceId(List.of(1001L)))
			.thenReturn(Map.of(1001L, "SOLO_CONCERT"));
		when(performanceMetadataLookupService.artistNamesByPerformanceId(List.of(1001L)))
			.thenReturn(Map.of(1001L, List.of("Artist A")));

		PageResponse<PerformanceRecommendationResponse> result = service.getRecommendedPerformances(
			1L, 501L, 0, 1, "matchedArtistCount,desc");

		assertThat(result.content()).singleElement().satisfies(response -> {
			assertThat(response.matchId()).isEqualTo(900L);
			assertThat(response.performanceName()).isEqualTo("공연명");
			assertThat(response.venueName()).isEqualTo("송도달빛축제공원");
			assertThat(response.region()).isEqualTo("인천");
			assertThat(response.artistNames()).containsExactly("Artist A");
			assertThat(response.performanceType()).isEqualTo("SOLO_CONCERT");
			assertThat(response.performanceStatus()).isEqualTo("ON_SALE");
			assertThat(response.minTicketPrice()).isEqualTo(80000);
		});
		assertThat(result.hasNext()).isTrue();
		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
		org.mockito.Mockito.verify(performanceMatchRepository)
			.findVisibleByAnalysisId(org.mockito.ArgumentMatchers.eq(501L), pageableCaptor.capture());
		assertThat(pageableCaptor.getValue().getSort().getOrderFor("matchedArtistCount"))
			.extracting(Sort.Order::getDirection)
			.isEqualTo(Sort.Direction.DESC);
	}

	@Test
	void hidesMissingOrAnotherUsersAnalysis() {
		when(playlistAnalysisRepository.existsByAnalysisIdAndUserId(501L, 2L)).thenReturn(false);

		assertThatThrownBy(() -> service.getRecommendedPerformances(
			2L, 501L, 0, 20, "matchPriority,asc"))
			.isInstanceOfSatisfying(BusinessException.class,
				exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
	}

	@Test
	void rejectsUnsupportedSort() {
		when(playlistAnalysisRepository.existsByAnalysisIdAndUserId(501L, 1L)).thenReturn(true);

		assertThatThrownBy(() -> service.getRecommendedPerformances(
			1L, 501L, 0, 20, "unknown,asc"))
			.isInstanceOfSatisfying(BusinessException.class,
				exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
	}

	@Test
	void returnsVisiblePerformanceWithVenue() {
		Performance performance = mock(Performance.class);
		when(performance.getPerformanceId()).thenReturn(1001L);
		when(performance.getPerformanceName()).thenReturn("2026 인천 펜타포트 록 페스티벌");
		when(performance.getPosterUrl()).thenReturn("https://images.example.com/performances/1001.jpg");
		when(performance.getStartDate()).thenReturn(java.time.LocalDate.of(2026, 8, 15));
		when(performance.getEndDate()).thenReturn(java.time.LocalDate.of(2026, 8, 17));
		when(performance.getBookingUrl()).thenReturn("https://tickets.example.com/performances/1001");
		when(performance.getTicketPriceText()).thenReturn("1일권 120,000원");
		when(performance.getRunningTime()).thenReturn("180분");
		when(performance.getAgeRestriction()).thenReturn("만 12세 이상");
		when(performance.getPerformanceStatus()).thenReturn(PerformanceStatus.ON_SALE);
		when(performance.getVenueId()).thenReturn(77L);
		when(performanceRepository.findByPerformanceIdAndIsDeletedFalse(1001L))
			.thenReturn(Optional.of(performance));

		Venue venue = mock(Venue.class);
		when(venue.getVenueId()).thenReturn(77L);
		when(venue.getVenueName()).thenReturn("송도달빛축제공원");
		when(venue.getCity()).thenReturn("인천");
		when(venueRepository.findById(77L)).thenReturn(Optional.of(venue));

		PerformanceArtist performanceArtist = mock(PerformanceArtist.class);
		when(performanceArtist.getArtistId()).thenReturn(7L);
		when(performanceArtist.getIsHeadliner()).thenReturn(false);
		when(performanceArtist.getLineupOrder()).thenReturn(1L);
		when(performanceArtistRepository.findByPerformanceIdOrderByLineupOrderAsc(1001L))
			.thenReturn(List.of(performanceArtist));
		com.setpik.server.artist.domain.Artist artist = mock(com.setpik.server.artist.domain.Artist.class);
		when(artist.getArtistId()).thenReturn(7L);
		when(artist.getArtistName()).thenReturn("Artist A");
		when(artist.getImageUrl()).thenReturn(null);
		when(artistRepository.findAllById(List.of(7L))).thenReturn(List.of(artist));
		when(artistAliasRepository.findByKopisArtistIdIn(java.util.Set.of(7L))).thenReturn(List.of(
			ArtistAlias.resolved(7L, "spotify-artist-a", "WIKIDATA", "Q1", java.time.LocalDateTime.now())
		));
		com.setpik.server.artist.domain.Artist spotifyArtist = mock(com.setpik.server.artist.domain.Artist.class);
		when(spotifyArtist.getSpotifyArtistId()).thenReturn("spotify-artist-a");
		when(spotifyArtist.getImageUrl()).thenReturn("https://images.example.com/artists/7.jpg");
		when(artistRepository.findBySpotifyArtistIdIn(any()))
			.thenReturn(List.of(spotifyArtist));

		PerformanceDetailResponse result = service.getPerformance(1001L);

		assertThat(result.performanceId()).isEqualTo(1001L);
		assertThat(result.performanceStatus()).isEqualTo("ON_SALE");
		assertThat(result.venue().venueName()).isEqualTo("송도달빛축제공원");
		assertThat(result.venue().city()).isEqualTo("인천");
		assertThat(result.ticketPriceText()).isEqualTo("1일권 120,000원");
		assertThat(result.runningTime()).isEqualTo("180분");
		assertThat(result.ageRestriction()).isEqualTo("만 12세 이상");
		assertThat(result.artists()).singleElement().satisfies(response -> {
			assertThat(response.artistId()).isEqualTo(7L);
			assertThat(response.artistName()).isEqualTo("Artist A");
			assertThat(response.artistImageUrl()).isEqualTo("https://images.example.com/artists/7.jpg");
			assertThat(response.lineupOrder()).isEqualTo(1L);
		});
	}

	@Test
	void returnsNotFoundForMissingOrDeletedPerformance() {
		when(performanceRepository.findByPerformanceIdAndIsDeletedFalse(1001L))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getPerformance(1001L))
			.isInstanceOfSatisfying(BusinessException.class,
				exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
	}

	@Test
	void returnsTicketSchedulesInOpeningOrderWithKstOffset() {
		when(performanceRepository.existsByPerformanceIdAndIsDeletedFalse(1001L)).thenReturn(true);
		TicketSchedule schedule = mock(TicketSchedule.class);
		when(schedule.getTicketScheduleId()).thenReturn(5001L);
		when(schedule.getScheduleName()).thenReturn("선예매");
		when(schedule.getSaleType()).thenReturn(TicketSaleType.PRE_SALE);
		when(schedule.getOpensAt()).thenReturn(java.time.LocalDateTime.of(2026, 8, 1, 20, 0));
		when(schedule.getClosesAt()).thenReturn(java.time.LocalDateTime.of(2026, 8, 1, 23, 59, 59));
		when(schedule.getSaleStatus()).thenReturn(SaleStatus.SCHEDULED);
		when(ticketScheduleRepository.findByPerformanceIdOrderByOpensAtAsc(1001L))
			.thenReturn(List.of(schedule));

		List<TicketScheduleResponse> result = service.getTicketSchedules(1001L);

		assertThat(result).singleElement().satisfies(response -> {
			assertThat(response.ticketScheduleId()).isEqualTo(5001L);
			assertThat(response.saleType()).isEqualTo("PRE_SALE");
			assertThat(response.opensAt().getOffset().toString()).isEqualTo("+09:00");
			assertThat(response.saleStatus()).isEqualTo("SCHEDULED");
		});
	}

	@Test
	void returnsNotFoundWhenTicketSchedulePerformanceDoesNotExist() {
		when(performanceRepository.existsByPerformanceIdAndIsDeletedFalse(1001L)).thenReturn(false);

		assertThatThrownBy(() -> service.getTicketSchedules(1001L))
			.isInstanceOfSatisfying(BusinessException.class,
				exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
	}

	@Test
	void returnsMatchedArtistsForOwnedAnalysisAndVisiblePerformance() {
		when(playlistAnalysisRepository.existsByAnalysisIdAndUserId(501L, 1L)).thenReturn(true);
		when(performanceRepository.existsByPerformanceIdAndIsDeletedFalse(1001L)).thenReturn(true);
		PerformanceMatch match = mock(PerformanceMatch.class);
		when(match.getMatchId()).thenReturn(900L);
		when(performanceMatchRepository.findByAnalysisIdAndPerformanceId(501L, 1001L))
			.thenReturn(Optional.of(match));

		PerformanceMatchArtist matchArtist = mock(PerformanceMatchArtist.class);
		when(matchArtist.getArtistId()).thenReturn(7L);
		when(matchArtist.getOccurrenceCount()).thenReturn(6);
		when(performanceMatchArtistRepository.findByMatchId(900L)).thenReturn(List.of(matchArtist));

		com.setpik.server.artist.domain.Artist artist = mock(com.setpik.server.artist.domain.Artist.class);
		when(artist.getArtistId()).thenReturn(7L);
		when(artist.getArtistName()).thenReturn("Artist A");
		when(artistRepository.findAllById(List.of(7L))).thenReturn(List.of(artist));

		PerformanceArtist performanceArtist = mock(PerformanceArtist.class);
		when(performanceArtist.getArtistId()).thenReturn(7L);
		when(performanceArtist.getIsHeadliner()).thenReturn(true);
		when(performanceArtistRepository.findByPerformanceIdOrderByLineupOrderAsc(1001L))
			.thenReturn(List.of(performanceArtist));

		List<MatchedArtistResponse> result = service.getMatchedArtists(1L, 501L, 1001L);

		assertThat(result).singleElement().satisfies(response -> {
			assertThat(response.artistId()).isEqualTo(7L);
			assertThat(response.artistName()).isEqualTo("Artist A");
			assertThat(response.occurrenceCount()).isEqualTo(6);
			assertThat(response.isHeadliner()).isTrue();
		});
	}

	@Test
	void hidesMatchedArtistsForAnotherUsersAnalysis() {
		when(playlistAnalysisRepository.existsByAnalysisIdAndUserId(501L, 2L)).thenReturn(false);

		assertThatThrownBy(() -> service.getMatchedArtists(2L, 501L, 1001L))
			.isInstanceOfSatisfying(BusinessException.class,
				exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
	}
}
