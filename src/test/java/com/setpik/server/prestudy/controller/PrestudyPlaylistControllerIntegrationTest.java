package com.setpik.server.prestudy.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.setpik.server.auth.security.JwtAccessTokenProvider;
import com.setpik.server.common.api.PageResponse;
import com.setpik.server.prestudy.dto.CreatePrestudyPlaylistRequest;
import com.setpik.server.prestudy.dto.CreatePrestudyPlaylistResponse;
import com.setpik.server.prestudy.dto.PrestudyCandidateResponse;
import com.setpik.server.prestudy.dto.PrestudyPlaylistDetailResponse;
import com.setpik.server.prestudy.dto.PrestudyPlaylistSummaryResponse;
import com.setpik.server.prestudy.dto.PrestudyPlaylistTrackResponse;
import com.setpik.server.prestudy.service.PrestudyPlaylistService;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PrestudyPlaylistControllerIntegrationTest {

	@Autowired private MockMvc mockMvc;
	@Autowired private JwtAccessTokenProvider accessTokenProvider;

	@MockitoBean private PrestudyPlaylistService prestudyPlaylistService;

	@Test
	void returnsPrestudyCandidatesUsingSpecificationShape() throws Exception {
		when(prestudyPlaylistService.getCandidates(1L, 1001L, 501L))
			.thenReturn(new PrestudyCandidateResponse(1001L, 501L, List.of(
				new PrestudyCandidateResponse.ArtistCandidate(
					7L, "Artist A", true, List.of(
						new PrestudyCandidateResponse.TrackCandidate(
							4001L, "Song A", "ORIGINAL_PLAYLIST")))
			)));

		mockMvc.perform(get("/api/v1/performances/{performanceId}/prestudy/candidates", 1001L)
				.param("analysisId", "501")
				.header(HttpHeaders.AUTHORIZATION, bearerToken(1L)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(1000))
			.andExpect(jsonPath("$.result.performanceId").value(1001))
			.andExpect(jsonPath("$.result.analysisId").value(501))
			.andExpect(jsonPath("$.result.artists[0].artistId").value(7))
			.andExpect(jsonPath("$.result.artists[0].isFromOriginalPlaylist").value(true))
			.andExpect(jsonPath("$.result.artists[0].candidateTracks[0].trackId").value(4001))
			.andExpect(jsonPath("$.result.artists[0].candidateTracks[0].sourceType")
				.value("ORIGINAL_PLAYLIST"));
	}

	@Test
	void createsPrestudyPlaylistUsingSpecificationShape() throws Exception {
		CreatePrestudyPlaylistRequest request = new CreatePrestudyPlaylistRequest(
			"펜타포트 예습 플레이리스트", false, 501L, List.of(4001L, 4002L));
		when(prestudyPlaylistService.createPrestudyPlaylist(1L, 1001L, request))
			.thenReturn(new CreatePrestudyPlaylistResponse(701L, "spotify-playlist", 2));

		mockMvc.perform(post("/api/v1/performances/{performanceId}/prestudy-playlists", 1001L)
				.header(HttpHeaders.AUTHORIZATION, bearerToken(1L))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "playlistTitle": "펜타포트 예습 플레이리스트",
					  "isPublic": false,
					  "analysisId": 501,
					  "selectedTrackIds": [4001, 4002]
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.code").value(1100))
			.andExpect(jsonPath("$.message").value("예습 플레이리스트가 생성되었습니다."))
			.andExpect(jsonPath("$.result.prestudyPlaylistId").value(701))
			.andExpect(jsonPath("$.result.spotifyPlaylistId").value("spotify-playlist"))
			.andExpect(jsonPath("$.result.trackCount").value(2));
	}

	@Test
	void returnsListDetailAndTracksUsingSpecificationShape() throws Exception {
		OffsetDateTime createdAt = OffsetDateTime.parse("2026-07-28T11:20:00+09:00");
		when(prestudyPlaylistService.getMyPrestudyPlaylists(eq(1L), any(Pageable.class)))
			.thenReturn(new PageResponse<>(List.of(new PrestudyPlaylistSummaryResponse(
				701L, "펜타포트 예습 플레이리스트", 1001L, "펜타포트",
				"poster.jpg", 15, "COMPLETED", createdAt)), 0, 20, 1, 1, false));
		when(prestudyPlaylistService.getPrestudyPlaylist(1L, 701L))
			.thenReturn(new PrestudyPlaylistDetailResponse(
				701L, "spotify-playlist", "펜타포트 예습 플레이리스트",
				false, 15, "COMPLETED", createdAt, false));
		when(prestudyPlaylistService.getPrestudyPlaylistTracks(1L, 701L))
			.thenReturn(List.of(new PrestudyPlaylistTrackResponse(
				4001L, "Song A", 1, "ORIGINAL_PLAYLIST", false)));

		mockMvc.perform(get("/api/v1/prestudy-playlists")
				.param("page", "0").param("size", "20").param("sort", "createdAt,desc")
				.header(HttpHeaders.AUTHORIZATION, bearerToken(1L)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.content[0].prestudyPlaylistId").value(701))
			.andExpect(jsonPath("$.result.content[0].performanceName").value("펜타포트"))
			.andExpect(jsonPath("$.result.content[0].creationStatus").value("COMPLETED"))
			.andExpect(jsonPath("$.result.content[0].createdAt")
				.value("2026-07-28T11:20:00+09:00"))
			.andExpect(jsonPath("$.result.page").value(0))
			.andExpect(jsonPath("$.result.hasNext").value(false));

		ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(prestudyPlaylistService).getMyPrestudyPlaylists(eq(1L), pageable.capture());
		org.assertj.core.api.Assertions.assertThat(
			pageable.getValue().getSort().getOrderFor("createdAt").getDirection().isDescending())
			.isTrue();

		mockMvc.perform(get("/api/v1/prestudy-playlists/{id}", 701L)
				.header(HttpHeaders.AUTHORIZATION, bearerToken(1L)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.spotifyPlaylistId").value("spotify-playlist"))
			.andExpect(jsonPath("$.result.spotifyDeleted").value(false));

		mockMvc.perform(get("/api/v1/prestudy-playlists/{id}/tracks", 701L)
				.header(HttpHeaders.AUTHORIZATION, bearerToken(1L)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result[0].trackId").value(4001))
			.andExpect(jsonPath("$.result[0].trackOrder").value(1))
			.andExpect(jsonPath("$.result[0].sourceType").value("ORIGINAL_PLAYLIST"))
			.andExpect(jsonPath("$.result[0].isNewArtistTrack").value(false));
	}

	@Test
	void validatesAuthenticationIdsPagingAndBody() throws Exception {
		mockMvc.perform(get("/api/v1/prestudy-playlists"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value(2001));

		String authorization = bearerToken(1L);
		mockMvc.perform(get("/api/v1/prestudy-playlists")
				.param("size", "101")
				.header(HttpHeaders.AUTHORIZATION, authorization))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(2000));
		mockMvc.perform(get("/api/v1/prestudy-playlists/0")
				.header(HttpHeaders.AUTHORIZATION, authorization))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(2000));
		mockMvc.perform(post("/api/v1/performances/1001/prestudy-playlists")
				.header(HttpHeaders.AUTHORIZATION, authorization)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"playlistTitle":"", "isPublic":false, "analysisId":0,
					 "selectedTrackIds":[]}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(2000));
	}

	private String bearerToken(Long userId) {
		return "Bearer " + accessTokenProvider.issue(userId);
	}
}
