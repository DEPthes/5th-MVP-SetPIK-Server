package com.setpik.server.favorite.controller;

import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.setpik.server.analysis.domain.AnalysisStatus;
import com.setpik.server.analysis.domain.PlaylistAnalysis;
import com.setpik.server.analysis.repository.PlaylistAnalysisRepository;
import com.setpik.server.artist.domain.Artist;
import com.setpik.server.artist.repository.ArtistRepository;
import com.setpik.server.auth.security.JwtAccessTokenProvider;
import com.setpik.server.favorite.domain.FavoritePerformance;
import com.setpik.server.favorite.repository.FavoritePerformanceRepository;
import com.setpik.server.member.domain.User;
import com.setpik.server.member.repository.UserRepository;
import com.setpik.server.performance.domain.Performance;
import com.setpik.server.performance.domain.PerformanceArtist;
import com.setpik.server.performance.domain.PerformanceMatch;
import com.setpik.server.performance.domain.PerformanceStatus;
import com.setpik.server.performance.domain.PerformanceType;
import com.setpik.server.performance.domain.PerformanceTypeMap;
import com.setpik.server.performance.domain.Venue;
import com.setpik.server.performance.repository.PerformanceArtistRepository;
import com.setpik.server.performance.repository.PerformanceMatchRepository;
import com.setpik.server.performance.repository.PerformanceRepository;
import com.setpik.server.performance.repository.PerformanceTypeMapRepository;
import com.setpik.server.performance.repository.PerformanceTypeRepository;
import com.setpik.server.performance.repository.VenueRepository;
import com.setpik.server.playlist.domain.SpotifyPlaylist;
import com.setpik.server.playlist.repository.SpotifyPlaylistRepository;
import com.setpik.server.prestudy.domain.PrestudyPlaylist;
import com.setpik.server.prestudy.repository.PrestudyPlaylistRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FavoritePerformanceControllerIntegrationTest {

	@Autowired private MockMvc mockMvc;
	@Autowired private UserRepository userRepository;
	@Autowired private VenueRepository venueRepository;
	@Autowired private PerformanceRepository performanceRepository;
	@Autowired private FavoritePerformanceRepository favoriteRepository;
	@Autowired private SpotifyPlaylistRepository playlistRepository;
	@Autowired private PlaylistAnalysisRepository analysisRepository;
	@Autowired private PerformanceMatchRepository performanceMatchRepository;
	@Autowired private PerformanceArtistRepository performanceArtistRepository;
	@Autowired private ArtistRepository artistRepository;
	@Autowired private PerformanceTypeRepository performanceTypeRepository;
	@Autowired private PerformanceTypeMapRepository performanceTypeMapRepository;
	@Autowired private PrestudyPlaylistRepository prestudyPlaylistRepository;
	@Autowired private JwtAccessTokenProvider accessTokenProvider;

	@Test
	void returnsOnlyActiveFavoritesForAuthenticatedUserInLatestOrder() throws Exception {
		LocalDateTime now = LocalDateTime.now();
		User user = userRepository.saveAndFlush(User.createActive(now));
		User otherUser = userRepository.saveAndFlush(User.createActive(now));
		Venue venue = venueRepository.saveAndFlush(new Venue(
			"favorite-venue", "송도달빛축제공원", "인천", null, null, null, null));
		Performance olderPerformance = performanceRepository.saveAndFlush(performance(
			"favorite-performance-older", "Older Festival", "older.jpg",
			LocalDate.of(2026, 8, 15), venue.getVenueId(), now));
		Performance latestPerformance = performanceRepository.saveAndFlush(performance(
			"favorite-performance-latest", "Latest Festival", "latest.jpg",
			LocalDate.of(2026, 9, 1), venue.getVenueId(), now, "R석 50,000원 / S석 30,000원"));
		Performance deletedPerformance = performanceRepository.saveAndFlush(performance(
			"favorite-performance-deleted", "Deleted Favorite Festival", "deleted.jpg",
			LocalDate.of(2026, 10, 1), venue.getVenueId(), now));

		favoriteRepository.saveAndFlush(new FavoritePerformance(
			user.getUserId(), olderPerformance.getPerformanceId(), now.minusDays(1)));
		FavoritePerformance latestFavorite = favoriteRepository.saveAndFlush(new FavoritePerformance(
			user.getUserId(), latestPerformance.getPerformanceId(), now));
		FavoritePerformance deletedFavorite = favoriteRepository.saveAndFlush(new FavoritePerformance(
			user.getUserId(), deletedPerformance.getPerformanceId(), now.plusMinutes(1)));
		deletedFavorite.delete(now.plusMinutes(2));
		favoriteRepository.saveAndFlush(new FavoritePerformance(
			otherUser.getUserId(), latestPerformance.getPerformanceId(), now.plusMinutes(3)));

		PerformanceType festivalType = performanceTypeRepository.saveAndFlush(
			new PerformanceType("FESTIVAL", "페스티벌"));
		performanceTypeMapRepository.saveAndFlush(
			new PerformanceTypeMap(latestPerformance.getPerformanceId(), festivalType.getPerformanceTypeId()));
		Artist headliner = artistRepository.saveAndFlush(Artist.fromKopis("Headliner Artist"));
		performanceArtistRepository.saveAndFlush(
			new PerformanceArtist(headliner.getArtistId(), latestPerformance.getPerformanceId(), 1L, true));

		SpotifyPlaylist playlist = playlistRepository.saveAndFlush(new SpotifyPlaylist(
			"spotify-favorite-match", "Favorite Match Playlist", null, null, false,
			"spotify-owner", "snapshot-favorite-match", 1, user.getUserId()));
		PlaylistAnalysis analysis = analysisRepository.saveAndFlush(new PlaylistAnalysis(
			user.getUserId(), playlist.getPlaylistId(), playlist.getSpotifyPlaylistId(),
			playlist.getPlaylistName(), playlist.getCoverImageUrl(), 1, 1,
			AnalysisStatus.COMPLETED, null));
		performanceMatchRepository.saveAndFlush(PerformanceMatch.create(
			(byte) 2, 2, 3, (byte) 66,
			"플레이리스트 속 아티스트 2팀이 이 공연에 출연합니다.",
			now, latestPerformance.getPerformanceId(), analysis.getAnalysisId(), null));
		PrestudyPlaylist prestudyPlaylist = prestudyPlaylistRepository.saveAndFlush(
			new PrestudyPlaylist(
				"Latest Festival 예습",
				false,
				user.getUserId(),
				latestPerformance.getPerformanceId(),
				analysis.getAnalysisId()));
		prestudyPlaylist.markCompleted("spotify-prestudy-701", 10);
		prestudyPlaylistRepository.flush();

		mockMvc.perform(get("/api/v1/favorites")
				.param("page", "0")
				.param("size", "1")
				.param("sort", "savedAt,desc")
				.header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.code").value(1000))
			.andExpect(jsonPath("$.message").value("요청에 성공했습니다."))
			.andExpect(jsonPath("$.result.content[0].favoriteId").value(latestFavorite.getFavoriteId()))
			.andExpect(jsonPath("$.result.content[0].performanceId").value(latestPerformance.getPerformanceId()))
			.andExpect(jsonPath("$.result.content[0].performanceName").value("Latest Festival"))
			.andExpect(jsonPath("$.result.content[0].posterUrl").value("latest.jpg"))
			.andExpect(jsonPath("$.result.content[0].startDate").value("2026-09-01"))
			.andExpect(jsonPath("$.result.content[0].venueName").value("송도달빛축제공원"))
			.andExpect(jsonPath("$.result.content[0].performanceType").value("FESTIVAL"))
			.andExpect(jsonPath("$.result.content[0].performanceStatus").value("SCHEDULED"))
			.andExpect(jsonPath("$.result.content[0].artistNames[0]").value("Headliner Artist"))
			.andExpect(jsonPath("$.result.content[0].matchedArtistCount").value(2))
			.andExpect(jsonPath("$.result.content[0].minTicketPrice").value(30000))
			.andExpect(jsonPath("$.result.content[0].prestudyPlaylistId")
				.value(prestudyPlaylist.getPrestudyPlaylistId()))
			.andExpect(jsonPath("$.result.content[0].creationStatus").value("COMPLETED"))
			.andExpect(jsonPath("$.result.content[0].spotifyPlaylistId").value("spotify-prestudy-701"))
			.andExpect(jsonPath("$.result.content[0].savedAt", endsWith("+09:00")))
			.andExpect(jsonPath("$.result.page").value(0))
			.andExpect(jsonPath("$.result.size").value(1))
			.andExpect(jsonPath("$.result.totalElements").value(2))
			.andExpect(jsonPath("$.result.totalPages").value(2))
			.andExpect(jsonPath("$.result.hasNext").value(true));
	}

	@Test
	void returnsZeroMatchedArtistCountWhenUserHasNoPlaylistAnalysis() throws Exception {
		LocalDateTime now = LocalDateTime.now();
		User newUser = userRepository.saveAndFlush(User.createActive(now));
		Venue venue = venueRepository.saveAndFlush(new Venue(
			"favorite-no-analysis-venue", "테스트 공연장", "서울", null, null, null, null));
		Performance performance = performanceRepository.saveAndFlush(performance(
			"favorite-no-analysis-performance", "No Analysis Festival", "poster.jpg",
			LocalDate.of(2026, 9, 1), venue.getVenueId(), now));
		favoriteRepository.saveAndFlush(new FavoritePerformance(
			newUser.getUserId(), performance.getPerformanceId(), now));

		mockMvc.perform(get("/api/v1/favorites")
				.param("page", "0")
				.param("size", "10")
				.header(HttpHeaders.AUTHORIZATION, bearerToken(newUser.getUserId())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.content[0].performanceId").value(performance.getPerformanceId()))
			.andExpect(jsonPath("$.result.content[0].matchedArtistCount").value(0));
	}

	@Test
	void ignoresFailedLatestAnalysisAndUsesPreviousCompletedAnalysisForMatchedArtistCount() throws Exception {
		LocalDateTime now = LocalDateTime.now();
		User user = userRepository.saveAndFlush(User.createActive(now));
		Venue venue = venueRepository.saveAndFlush(new Venue(
			"favorite-failed-analysis-venue", "테스트 공연장", "서울", null, null, null, null));
		Performance performance = performanceRepository.saveAndFlush(performance(
			"favorite-failed-analysis-performance", "Failed Analysis Festival", "poster.jpg",
			LocalDate.of(2026, 9, 1), venue.getVenueId(), now));
		favoriteRepository.saveAndFlush(new FavoritePerformance(
			user.getUserId(), performance.getPerformanceId(), now));

		SpotifyPlaylist playlist = playlistRepository.saveAndFlush(new SpotifyPlaylist(
			"spotify-failed-analysis", "Failed Analysis Playlist", null, null, false,
			"spotify-owner", "snapshot-failed-analysis", 1, user.getUserId()));
		PlaylistAnalysis completedAnalysis = analysisRepository.saveAndFlush(new PlaylistAnalysis(
			user.getUserId(), playlist.getPlaylistId(), playlist.getSpotifyPlaylistId(),
			playlist.getPlaylistName(), playlist.getCoverImageUrl(), 1, 1,
			AnalysisStatus.COMPLETED, null));
		performanceMatchRepository.saveAndFlush(PerformanceMatch.create(
			(byte) 2, 2, 3, (byte) 66,
			"플레이리스트 속 아티스트 2팀이 이 공연에 출연합니다.",
			now, performance.getPerformanceId(), completedAnalysis.getAnalysisId(), null));

		// COMPLETED 분석보다 나중에 생성된 FAILED 분석이 최신 분석 판정에서 제외되어야 한다.
		analysisRepository.saveAndFlush(new PlaylistAnalysis(
			user.getUserId(), playlist.getPlaylistId(), playlist.getSpotifyPlaylistId(),
			playlist.getPlaylistName(), playlist.getCoverImageUrl(), 1, 1,
			AnalysisStatus.FAILED, "분석 실패"));

		mockMvc.perform(get("/api/v1/favorites")
				.param("page", "0")
				.param("size", "10")
				.header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.content[0].performanceId").value(performance.getPerformanceId()))
			.andExpect(jsonPath("$.result.content[0].matchedArtistCount").value(2));
	}

	@Test
	void createsFavoriteRejectsDuplicateAndRestoresDeletedFavorite() throws Exception {
		LocalDateTime now = LocalDateTime.now();
		User user = userRepository.saveAndFlush(User.createActive(now));
		Venue venue = venueRepository.saveAndFlush(new Venue(
			"favorite-create-venue", "테스트 공연장", "서울", null, null, null, null));
		Performance performance = performanceRepository.saveAndFlush(performance(
			"favorite-create-performance", "Favorite Concert", "poster.jpg",
			LocalDate.of(2026, 11, 1), venue.getVenueId(), now));
		String authorization = bearerToken(user.getUserId());
		String request = "{\"performanceId\":" + performance.getPerformanceId() + "}";

		mockMvc.perform(post("/api/v1/favorites")
				.header(HttpHeaders.AUTHORIZATION, authorization)
				.contentType(MediaType.APPLICATION_JSON)
				.content(request))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.code").value(1100))
			.andExpect(jsonPath("$.message").value("관심 공연이 저장되었습니다."))
			.andExpect(jsonPath("$.result.favoriteId").isNumber());

		FavoritePerformance favorite = favoriteRepository
			.findByUserIdAndPerformanceId(user.getUserId(), performance.getPerformanceId())
			.orElseThrow();
		Long favoriteId = favorite.getFavoriteId();
		Performance updatedPerformance = performanceRepository
			.findById(performance.getPerformanceId()).orElseThrow();
		org.assertj.core.api.Assertions.assertThat(updatedPerformance.getFavoriteCount()).isEqualTo(1);

		mockMvc.perform(post("/api/v1/favorites")
				.header(HttpHeaders.AUTHORIZATION, authorization)
				.contentType(MediaType.APPLICATION_JSON)
				.content(request))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value(2004));

		favorite.delete(now.plusMinutes(1));
		updatedPerformance.decreaseFavoriteCount();
		favoriteRepository.flush();
		mockMvc.perform(post("/api/v1/favorites")
				.header(HttpHeaders.AUTHORIZATION, authorization)
				.contentType(MediaType.APPLICATION_JSON)
				.content(request))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.result.favoriteId").value(favoriteId));
		org.assertj.core.api.Assertions.assertThat(
			performanceRepository.findById(performance.getPerformanceId()).orElseThrow().getFavoriteCount()
		).isEqualTo(1);
	}

	@Test
	void validatesAuthenticationAndPagingParameters() throws Exception {
		mockMvc.perform(get("/api/v1/favorites"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value(2001));

		User user = userRepository.saveAndFlush(User.createActive(LocalDateTime.now()));
		String authorization = bearerToken(user.getUserId());
		mockMvc.perform(get("/api/v1/favorites")
				.param("size", "101")
				.header(HttpHeaders.AUTHORIZATION, authorization))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(2000));

		mockMvc.perform(get("/api/v1/favorites")
				.param("sort", "unknown,asc")
				.header(HttpHeaders.AUTHORIZATION, authorization))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(2000));

		mockMvc.perform(post("/api/v1/favorites")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"performanceId\":1}"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value(2001));

		mockMvc.perform(post("/api/v1/favorites")
				.header(HttpHeaders.AUTHORIZATION, authorization)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"performanceId\":0}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(2000));

		mockMvc.perform(post("/api/v1/favorites")
				.header(HttpHeaders.AUTHORIZATION, authorization)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"performanceId\":999999}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(2000));
	}

	@Test
	void deletesOnlyOwnedFavoriteAndDecreasesFavoriteCount() throws Exception {
		LocalDateTime now = LocalDateTime.now();
		User user = userRepository.saveAndFlush(User.createActive(now));
		User otherUser = userRepository.saveAndFlush(User.createActive(now));
		Venue venue = venueRepository.saveAndFlush(new Venue(
			"favorite-delete-venue", "테스트 공연장", "서울", null, null, null, null));
		Performance performance = performanceRepository.saveAndFlush(performance(
			"favorite-delete-performance", "Delete Concert", "poster.jpg",
			LocalDate.of(2026, 12, 1), venue.getVenueId(), now));
		performance.increaseFavoriteCount();
		FavoritePerformance favorite = favoriteRepository.saveAndFlush(new FavoritePerformance(
			user.getUserId(), performance.getPerformanceId(), now));

		mockMvc.perform(delete("/api/v1/favorites/{favoriteId}", favorite.getFavoriteId())
				.header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.code").value(1000))
			.andExpect(jsonPath("$.message").value("관심 공연이 삭제되었습니다."))
			.andExpect(jsonPath("$.result").doesNotExist());

		org.assertj.core.api.Assertions.assertThat(
			favoriteRepository.findById(favorite.getFavoriteId()).orElseThrow().getDeletedAt()
		).isNotNull();
		org.assertj.core.api.Assertions.assertThat(
			performanceRepository.findById(performance.getPerformanceId()).orElseThrow().getFavoriteCount()
		).isZero();

		mockMvc.perform(delete("/api/v1/favorites/{favoriteId}", favorite.getFavoriteId())
				.header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value(2004));

		mockMvc.perform(delete("/api/v1/favorites/{favoriteId}", favorite.getFavoriteId())
				.header(HttpHeaders.AUTHORIZATION, bearerToken(otherUser.getUserId())))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value(2003));
	}

	@Test
	void validatesFavoriteDeletionRequest() throws Exception {
		User user = userRepository.saveAndFlush(User.createActive(LocalDateTime.now()));

		mockMvc.perform(delete("/api/v1/favorites/1"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value(2001));

		mockMvc.perform(delete("/api/v1/favorites/0")
				.header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(2000));

		mockMvc.perform(delete("/api/v1/favorites/999999")
				.header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value(2003));
	}

	private Performance performance(
		String kopisId,
		String name,
		String posterUrl,
		LocalDate startDate,
		Long venueId,
		LocalDateTime syncedAt
	) {
		return performance(kopisId, name, posterUrl, startDate, venueId, syncedAt, null);
	}

	private Performance performance(
		String kopisId,
		String name,
		String posterUrl,
		LocalDate startDate,
		Long venueId,
		LocalDateTime syncedAt,
		String ticketPriceText
	) {
		return new Performance(
			kopisId, name, startDate, startDate.plusDays(1), posterUrl, null,
			PerformanceStatus.SCHEDULED, "PAID", ticketPriceText, syncedAt, venueId);
	}

	private String bearerToken(Long userId) {
		return "Bearer " + accessTokenProvider.issue(userId);
	}
}
