package com.setpik.server.performanceview.controller;

import static org.hamcrest.Matchers.endsWith;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.setpik.server.analysis.domain.AnalysisStatus;
import com.setpik.server.analysis.domain.PlaylistAnalysis;
import com.setpik.server.analysis.repository.PlaylistAnalysisRepository;
import com.setpik.server.auth.security.JwtAccessTokenProvider;
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
import com.setpik.server.performance.repository.PerformanceRepository;
import com.setpik.server.performance.repository.PerformanceMatchRepository;
import com.setpik.server.performance.repository.PerformanceTypeMapRepository;
import com.setpik.server.performance.repository.PerformanceTypeRepository;
import com.setpik.server.performance.repository.VenueRepository;
import com.setpik.server.artist.domain.Artist;
import com.setpik.server.artist.repository.ArtistRepository;
import com.setpik.server.performanceview.domain.PerformanceView;
import com.setpik.server.performanceview.repository.PerformanceViewRepository;
import com.setpik.server.playlist.domain.SpotifyPlaylist;
import com.setpik.server.playlist.repository.SpotifyPlaylistRepository;
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
class PerformanceViewControllerIntegrationTest {

	@Autowired private MockMvc mockMvc;
	@Autowired private UserRepository userRepository;
	@Autowired private SpotifyPlaylistRepository playlistRepository;
	@Autowired private PlaylistAnalysisRepository analysisRepository;
	@Autowired private VenueRepository venueRepository;
	@Autowired private PerformanceRepository performanceRepository;
	@Autowired private PerformanceMatchRepository performanceMatchRepository;
	@Autowired private PerformanceArtistRepository performanceArtistRepository;
	@Autowired private ArtistRepository artistRepository;
	@Autowired private PerformanceTypeRepository performanceTypeRepository;
	@Autowired private PerformanceTypeMapRepository performanceTypeMapRepository;
	@Autowired private PerformanceViewRepository performanceViewRepository;
	@Autowired private JwtAccessTokenProvider accessTokenProvider;

	@Test
	void returnsOnlyAuthenticatedUsersViewsInLatestOrder() throws Exception {
		LocalDateTime now = LocalDateTime.now();
		User user = userRepository.saveAndFlush(User.createActive(now));
		User otherUser = userRepository.saveAndFlush(User.createActive(now));
		SpotifyPlaylist playlist = playlistRepository.saveAndFlush(new SpotifyPlaylist(
			"spotify-view-history", "View History Playlist", null, null, false,
			"spotify-owner", "snapshot-view-history", 1, user.getUserId()));
		PlaylistAnalysis analysis = analysisRepository.saveAndFlush(new PlaylistAnalysis(
			user.getUserId(), playlist.getPlaylistId(), playlist.getSpotifyPlaylistId(),
			playlist.getPlaylistName(), playlist.getCoverImageUrl(), 1, 1,
			AnalysisStatus.COMPLETED, null));
		Venue venue = venueRepository.saveAndFlush(new Venue(
			"view-history-venue", "송도달빛축제공원", "인천", null, null, null, null));
		Performance olderPerformance = performanceRepository.saveAndFlush(performance(
			"view-history-older", "Older Festival", "older.jpg",
			LocalDate.of(2026, 8, 15), venue.getVenueId(), now));
		Performance latestPerformance = performanceRepository.saveAndFlush(performance(
			"view-history-latest", "Latest Festival", "latest.jpg",
			LocalDate.of(2026, 9, 1), venue.getVenueId(), now));

		performanceViewRepository.saveAndFlush(new PerformanceView(
			user.getUserId(), analysis.getAnalysisId(), olderPerformance.getPerformanceId(),
			now.minusDays(1)));
		PerformanceView latestView = performanceViewRepository.saveAndFlush(new PerformanceView(
			user.getUserId(), analysis.getAnalysisId(), latestPerformance.getPerformanceId(), now));
		performanceMatchRepository.saveAndFlush(PerformanceMatch.create(
			(byte) 2, 1, 3, (byte) 33,
			"플레이리스트 속 아티스트 1팀이 이 공연에 출연합니다.",
			now, latestPerformance.getPerformanceId(), analysis.getAnalysisId(), null));
		performanceViewRepository.saveAndFlush(new PerformanceView(
			otherUser.getUserId(), analysis.getAnalysisId(), latestPerformance.getPerformanceId(),
			now.plusDays(1)));

		PerformanceType festivalType = performanceTypeRepository.saveAndFlush(
			new PerformanceType("FESTIVAL", "페스티벌"));
		performanceTypeMapRepository.saveAndFlush(
			new PerformanceTypeMap(latestPerformance.getPerformanceId(), festivalType.getPerformanceTypeId()));
		Artist headliner = artistRepository.saveAndFlush(
			Artist.fromKopis("Headliner Artist"));
		performanceArtistRepository.saveAndFlush(
			new PerformanceArtist(headliner.getArtistId(), latestPerformance.getPerformanceId(), 1L, true));

		mockMvc.perform(get("/api/v1/performance-views")
				.param("page", "0")
				.param("size", "1")
				.param("sort", "viewedAt,desc")
				.header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.code").value(1000))
			.andExpect(jsonPath("$.message").value("요청에 성공했습니다."))
			.andExpect(jsonPath("$.result.content[0].viewId").value(latestView.getViewId()))
			.andExpect(jsonPath("$.result.content[0].performanceId")
				.value(latestPerformance.getPerformanceId()))
			.andExpect(jsonPath("$.result.content[0].performanceName").value("Latest Festival"))
			.andExpect(jsonPath("$.result.content[0].posterUrl").value("latest.jpg"))
			.andExpect(jsonPath("$.result.content[0].startDate").value("2026-09-01"))
			.andExpect(jsonPath("$.result.content[0].venueName").value("송도달빛축제공원"))
			.andExpect(jsonPath("$.result.content[0].performanceType").value("FESTIVAL"))
			.andExpect(jsonPath("$.result.content[0].performanceStatus").value("SCHEDULED"))
			.andExpect(jsonPath("$.result.content[0].artistNames[0]").value("Headliner Artist"))
			.andExpect(jsonPath("$.result.content[0].analysisId").value(analysis.getAnalysisId()))
			.andExpect(jsonPath("$.result.content[0].matchedArtistCount").value(1))
			.andExpect(jsonPath("$.result.content[0].viewedAt", endsWith("+09:00")))
			.andExpect(jsonPath("$.result.page").value(0))
			.andExpect(jsonPath("$.result.size").value(1))
			.andExpect(jsonPath("$.result.totalElements").value(2))
			.andExpect(jsonPath("$.result.totalPages").value(2))
			.andExpect(jsonPath("$.result.hasNext").value(true));
	}

	@Test
	void validatesAuthenticationAndPagingParameters() throws Exception {
		mockMvc.perform(get("/api/v1/performance-views"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value(2001));

		User user = userRepository.saveAndFlush(User.createActive(LocalDateTime.now()));
		String authorization = bearerToken(user.getUserId());
		mockMvc.perform(get("/api/v1/performance-views")
				.param("size", "101")
				.header(HttpHeaders.AUTHORIZATION, authorization))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(2000));

		mockMvc.perform(get("/api/v1/performance-views")
				.param("sort", "unknown,asc")
				.header(HttpHeaders.AUTHORIZATION, authorization))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(2000));
	}

	@Test
	void createsAndUpdatesPerformanceViewForOwnedAnalysis() throws Exception {
		LocalDateTime now = LocalDateTime.now();
		User user = userRepository.saveAndFlush(User.createActive(now));
		User otherUser = userRepository.saveAndFlush(User.createActive(now));
		SpotifyPlaylist playlist = playlistRepository.saveAndFlush(new SpotifyPlaylist(
			"spotify-view-upsert", "View Upsert Playlist", null, null, false,
			"spotify-owner", "snapshot-view-upsert", 1, user.getUserId()));
		PlaylistAnalysis analysis = analysisRepository.saveAndFlush(new PlaylistAnalysis(
			user.getUserId(), playlist.getPlaylistId(), playlist.getSpotifyPlaylistId(),
			playlist.getPlaylistName(), playlist.getCoverImageUrl(), 1, 1,
			AnalysisStatus.COMPLETED, null));
		Venue venue = venueRepository.saveAndFlush(new Venue(
			"view-upsert-venue", "테스트 공연장", "서울", null, null, null, null));
		Performance performance = performanceRepository.saveAndFlush(performance(
			"view-upsert-performance", "Upsert Concert", "poster.jpg",
			LocalDate.of(2026, 12, 1), venue.getVenueId(), now));
		String request = "{\"performanceId\":" + performance.getPerformanceId()
			+ ",\"analysisId\":" + analysis.getAnalysisId() + "}";

		mockMvc.perform(post("/api/v1/performance-views")
				.header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(request))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.code").value(1000))
			.andExpect(jsonPath("$.message").value("조회 기록이 저장 또는 갱신되었습니다."))
			.andExpect(jsonPath("$.result.viewId").isNumber())
			.andExpect(jsonPath("$.result.created").value(true))
			.andExpect(jsonPath("$.result.viewedAt", endsWith("+09:00")));

		PerformanceView savedView = performanceViewRepository
			.findByUserIdAndAnalysisIdAndPerformanceId(
				user.getUserId(), analysis.getAnalysisId(), performance.getPerformanceId())
			.orElseThrow();
		Long viewId = savedView.getViewId();
		savedView.updateViewedAt(now.minusDays(1));
		performanceViewRepository.flush();

		mockMvc.perform(post("/api/v1/performance-views")
				.header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(request))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.viewId").value(viewId))
			.andExpect(jsonPath("$.result.created").value(false))
			.andExpect(jsonPath("$.result.viewedAt", endsWith("+09:00")));

		PerformanceView updatedView = performanceViewRepository.findById(viewId).orElseThrow();
		assertThat(updatedView.getViewedAt()).isAfter(now.minusDays(1));
		assertThat(performanceViewRepository.count()).isEqualTo(1);

		mockMvc.perform(post("/api/v1/performance-views")
				.header(HttpHeaders.AUTHORIZATION, bearerToken(otherUser.getUserId()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(request))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value(2003));
	}

	@Test
	void keepsOnlyFiftyMostRecentPerformanceViewsPerUser() throws Exception {
		LocalDateTime now = LocalDateTime.now();
		User user = userRepository.saveAndFlush(User.createActive(now));
		SpotifyPlaylist playlist = playlistRepository.saveAndFlush(new SpotifyPlaylist(
			"spotify-view-limit", "View Limit Playlist", null, null, false,
			"spotify-owner", "snapshot-view-limit", 1, user.getUserId()));
		PlaylistAnalysis analysis = analysisRepository.saveAndFlush(new PlaylistAnalysis(
			user.getUserId(), playlist.getPlaylistId(), playlist.getSpotifyPlaylistId(),
			playlist.getPlaylistName(), playlist.getCoverImageUrl(), 1, 1,
			AnalysisStatus.COMPLETED, null));
		Venue venue = venueRepository.saveAndFlush(new Venue(
			"view-limit-venue", "조회 제한 공연장", "서울", null, null, null, null));

		Long oldestViewId = null;
		for (int index = 0; index < 50; index++) {
			Performance performance = performanceRepository.saveAndFlush(performance(
				"view-limit-performance-" + index,
				"View Limit Performance " + index,
				null,
				LocalDate.of(2026, 9, 1).plusDays(index),
				venue.getVenueId(),
				now
			));
			PerformanceView view = performanceViewRepository.saveAndFlush(new PerformanceView(
				user.getUserId(), analysis.getAnalysisId(), performance.getPerformanceId(),
				now.minusMinutes(50L - index)));
			if (index == 0) {
				oldestViewId = view.getViewId();
			}
		}

		Performance latestPerformance = performanceRepository.saveAndFlush(performance(
			"view-limit-performance-latest", "Latest View Limit Performance", null,
			LocalDate.of(2026, 12, 31), venue.getVenueId(), now));
		String request = "{\"performanceId\":" + latestPerformance.getPerformanceId()
			+ ",\"analysisId\":" + analysis.getAnalysisId() + "}";

		mockMvc.perform(post("/api/v1/performance-views")
				.header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(request))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.created").value(true));

		var views = performanceViewRepository
			.findByUserIdOrderByViewedAtDescViewIdDesc(user.getUserId());
		assertThat(views).hasSize(50);
		assertThat(views).extracting(PerformanceView::getViewId)
			.doesNotContain(oldestViewId);
	}

	@Test
	void validatesPerformanceViewSaveRequest() throws Exception {
		LocalDateTime now = LocalDateTime.now();
		User user = userRepository.saveAndFlush(User.createActive(now));

		mockMvc.perform(post("/api/v1/performance-views")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"performanceId\":1,\"analysisId\":1}"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value(2001));

		mockMvc.perform(post("/api/v1/performance-views")
				.header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"performanceId\":0,\"analysisId\":0}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(2000));

		mockMvc.perform(post("/api/v1/performance-views")
				.header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"performanceId\":999999,\"analysisId\":999999}"))
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
		return new Performance(
			kopisId, name, startDate, startDate.plusDays(1), posterUrl, null,
			PerformanceStatus.SCHEDULED, "PAID", null, syncedAt, venueId);
	}

	private String bearerToken(Long userId) {
		return "Bearer " + accessTokenProvider.issue(userId);
	}
}
