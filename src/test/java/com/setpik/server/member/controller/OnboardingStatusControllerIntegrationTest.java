package com.setpik.server.member.controller;

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
import com.setpik.server.playlist.domain.PlaylistRecentSelection;
import com.setpik.server.playlist.domain.SpotifyPlaylist;
import com.setpik.server.playlist.repository.PlaylistRecentSelectionRepository;
import com.setpik.server.playlist.repository.SpotifyPlaylistRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OnboardingStatusControllerIntegrationTest {

	@Autowired private MockMvc mockMvc;
	@Autowired private UserRepository userRepository;
	@Autowired private SpotifyPlaylistRepository playlistRepository;
	@Autowired private PlaylistRecentSelectionRepository recentSelectionRepository;
	@Autowired private PlaylistAnalysisRepository analysisRepository;
	@Autowired private JwtAccessTokenProvider accessTokenProvider;

	@Test
	void returnsNotStartedWhenPlaylistHasNotBeenSelected() throws Exception {
		User user = userRepository.saveAndFlush(User.createActive(LocalDateTime.now()));

		mockMvc.perform(get("/api/v1/users/me/onboarding-status")
				.header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.status").value("NOT_STARTED"))
			.andExpect(jsonPath("$.result.selectedPlaylistId").doesNotExist())
			.andExpect(jsonPath("$.result.analysisId").doesNotExist());
	}

	@Test
	void changesFromPlaylistSelectedToCompletedAfterArtistSelectionConfirmation() throws Exception {
		LocalDateTime now = LocalDateTime.now();
		User user = userRepository.saveAndFlush(User.createActive(now));
		SpotifyPlaylist playlist = playlistRepository.saveAndFlush(new SpotifyPlaylist(
			"spotify-onboarding", "Onboarding Playlist", null, null, false,
			"spotify-owner", "snapshot-onboarding", 10, user.getUserId()));
		recentSelectionRepository.saveAndFlush(new PlaylistRecentSelection(
			user.getUserId(), playlist.getPlaylistId(), now));
		PlaylistAnalysis analysis = analysisRepository.saveAndFlush(new PlaylistAnalysis(
			user.getUserId(), playlist.getPlaylistId(), playlist.getSpotifyPlaylistId(),
			playlist.getPlaylistName(), null, 10, 3, AnalysisStatus.COMPLETED, null));
		String authorization = bearerToken(user.getUserId());

		mockMvc.perform(get("/api/v1/users/me/onboarding-status")
				.header(HttpHeaders.AUTHORIZATION, authorization))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.status").value("PLAYLIST_SELECTED"))
			.andExpect(jsonPath("$.result.selectedPlaylistId").value(playlist.getPlaylistId()))
			.andExpect(jsonPath("$.result.analysisId").value(analysis.getAnalysisId()));

		mockMvc.perform(post("/api/v1/analyses/{analysisId}/artist-selection/complete",
				analysis.getAnalysisId())
				.header(HttpHeaders.AUTHORIZATION, authorization))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("관심 아티스트 선택이 완료되었습니다."))
			.andExpect(jsonPath("$.result.analysisId").value(analysis.getAnalysisId()))
			.andExpect(jsonPath("$.result.completedAt").isNotEmpty());

		mockMvc.perform(get("/api/v1/users/me/onboarding-status")
				.header(HttpHeaders.AUTHORIZATION, authorization))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.status").value("COMPLETED"))
			.andExpect(jsonPath("$.result.selectedPlaylistId").value(playlist.getPlaylistId()))
			.andExpect(jsonPath("$.result.analysisId").value(analysis.getAnalysisId()));
	}

	private String bearerToken(Long userId) {
		return "Bearer " + accessTokenProvider.issue(userId);
	}
}
