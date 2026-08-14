package com.setpik.server.analysis.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.setpik.server.analysis.domain.AnalysisArtist;
import com.setpik.server.analysis.domain.PlaylistAnalysis;
import com.setpik.server.analysis.repository.AnalysisArtistRepository;
import com.setpik.server.analysis.repository.PlaylistAnalysisRepository;
import com.setpik.server.artist.domain.Artist;
import com.setpik.server.artist.repository.ArtistRepository;
import com.setpik.server.auth.security.JwtAccessTokenProvider;
import com.setpik.server.member.domain.User;
import com.setpik.server.member.repository.UserRepository;
import com.setpik.server.playlist.domain.PlaylistTrack;
import com.setpik.server.playlist.domain.SpotifyPlaylist;
import com.setpik.server.playlist.domain.Track;
import com.setpik.server.playlist.domain.TrackArtist;
import com.setpik.server.playlist.repository.PlaylistTrackRepository;
import com.setpik.server.playlist.repository.SpotifyPlaylistRepository;
import com.setpik.server.playlist.repository.TrackArtistRepository;
import com.setpik.server.playlist.repository.TrackRepository;
import java.time.LocalDateTime;
import java.util.List;
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
class AnalysisControllerIntegrationTest {

	@Autowired private MockMvc mockMvc;
	@Autowired private UserRepository userRepository;
	@Autowired private SpotifyPlaylistRepository playlistRepository;
	@Autowired private TrackRepository trackRepository;
	@Autowired private PlaylistTrackRepository playlistTrackRepository;
	@Autowired private ArtistRepository artistRepository;
	@Autowired private TrackArtistRepository trackArtistRepository;
	@Autowired private PlaylistAnalysisRepository analysisRepository;
	@Autowired private AnalysisArtistRepository analysisArtistRepository;
	@Autowired private JwtAccessTokenProvider accessTokenProvider;

	@Test
	void analyzesPlaylistTracksAndCountsDistinctArtists() throws Exception {
		LocalDateTime now = LocalDateTime.now();
		User user = userRepository.saveAndFlush(User.createActive(now));
		SpotifyPlaylist playlist = playlistRepository.saveAndFlush(new SpotifyPlaylist(
			"spotify-analysis", "Analysis Playlist", null, null, false,
			"spotify-owner", "snapshot-analysis", 3, user.getUserId()
		));
		Track firstTrack = trackRepository.saveAndFlush(new Track(
			"spotify-analysis-track-1", "Track 1", null, null, null, null, 180000, true));
		Track secondTrack = trackRepository.saveAndFlush(new Track(
			"spotify-analysis-track-2", "Track 2", null, null, null, null, 180000, true));
		Artist repeatedArtist = artistRepository.saveAndFlush(
			new Artist("spotify-analysis-artist-1", "Repeated Artist", null));
		Artist singleArtist = artistRepository.saveAndFlush(
			new Artist("spotify-analysis-artist-2", "Single Artist", null));

		playlistTrackRepository.saveAllAndFlush(List.of(
			new PlaylistTrack(playlist.getPlaylistId(), firstTrack.getTrackId(), 1, now),
			new PlaylistTrack(playlist.getPlaylistId(), secondTrack.getTrackId(), 2, now),
			new PlaylistTrack(playlist.getPlaylistId(), firstTrack.getTrackId(), 3, now)
		));
		trackArtistRepository.saveAllAndFlush(List.of(
			new TrackArtist(firstTrack.getTrackId(), repeatedArtist.getArtistId(), (short) 1),
			new TrackArtist(firstTrack.getTrackId(), singleArtist.getArtistId(), (short) 2),
			new TrackArtist(secondTrack.getTrackId(), repeatedArtist.getArtistId(), (short) 1)
		));

		mockMvc.perform(post("/api/v1/playlists/{playlistId}/analysis", playlist.getPlaylistId())
				.header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.code").value(1100))
			.andExpect(jsonPath("$.message").value("플레이리스트 분석이 완료되었습니다."))
			.andExpect(jsonPath("$.result.analysisStatus").value("COMPLETED"))
			.andExpect(jsonPath("$.result.totalTrackCount").value(3))
			.andExpect(jsonPath("$.result.selectedArtistCount").value(2))
			.andExpect(jsonPath("$.result.analyzedAt", endsWith("+09:00")));

		mockMvc.perform(post("/api/v1/playlists/{playlistId}/analysis", playlist.getPlaylistId())
				.header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
			.andExpect(status().isCreated());

		PlaylistAnalysis analysis = analysisRepository
			.findFirstByPlaylistIdAndUserIdOrderByAnalyzedAtDescAnalysisIdDesc(
				playlist.getPlaylistId(), user.getUserId())
			.orElseThrow();
		List<AnalysisArtist> artists = analysisArtistRepository
			.findByAnalysisIdOrderByDisplayRankAsc(analysis.getAnalysisId());
		assertThat(artists).extracting(AnalysisArtist::getOccurrenceCount)
			.containsExactly(3, 2);

		mockMvc.perform(get("/api/v1/playlists/{playlistId}/analysis", playlist.getPlaylistId())
				.header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.code").value(1000))
			.andExpect(jsonPath("$.message").value("요청에 성공했습니다."))
			.andExpect(jsonPath("$.result.analysisId").value(analysis.getAnalysisId()))
			.andExpect(jsonPath("$.result.analysisStatus").value("COMPLETED"))
			.andExpect(jsonPath("$.result.warningMessage").isEmpty())
			.andExpect(jsonPath("$.result.selectedArtistCount").value(2))
			.andExpect(jsonPath("$.result.topArtists[0].artistId").value(repeatedArtist.getArtistId()))
			.andExpect(jsonPath("$.result.topArtists[0].artistName").value("Repeated Artist"))
			.andExpect(jsonPath("$.result.topArtists[0].occurrenceCount").value(3))
			.andExpect(jsonPath("$.result.topArtists[0].isMajor").value(true))
			.andExpect(jsonPath("$.result.topArtists[0].isExcluded").value(false))
			.andExpect(jsonPath("$.result.topArtists[0]", not(hasKey("popularitySnapshot"))))
			.andExpect(jsonPath("$.result.topArtists[0]", not(hasKey("displayRank"))));

		String updateRequest = """
			{
			  "artists": [
			    {"artistId": %d, "isExcluded": true},
			    {"artistId": %d, "isExcluded": false}
			  ]
			}
			""".formatted(repeatedArtist.getArtistId(), singleArtist.getArtistId());
		mockMvc.perform(patch("/api/v1/analyses/{analysisId}/artists", analysis.getAnalysisId())
				.header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(updateRequest))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.code").value(1000))
			.andExpect(jsonPath("$.message").value("분석 아티스트 상태가 수정되었습니다."))
			.andExpect(jsonPath("$.result.analysisId").value(analysis.getAnalysisId()))
			.andExpect(jsonPath("$.result.updatedArtistCount").value(2));

		// 같은 PATCH 요청을 반복해도 상태는 동일하며 요청한 두 항목을 정상 처리한다.
		mockMvc.perform(patch("/api/v1/analyses/{analysisId}/artists", analysis.getAnalysisId())
				.header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(updateRequest))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.updatedArtistCount").value(2));

		PlaylistAnalysis updatedAnalysis = analysisRepository.findById(analysis.getAnalysisId()).orElseThrow();
		assertThat(updatedAnalysis.getSelectedArtistCount()).isEqualTo(1);

		mockMvc.perform(get("/api/v1/analyses/{analysisId}/artists", analysis.getAnalysisId())
				.param("includeExcluded", "false")
				.param("page", "0")
				.param("size", "20")
				.param("sort", "displayRank,asc")
				.header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.code").value(1000))
			.andExpect(jsonPath("$.message").value("요청에 성공했습니다."))
			.andExpect(jsonPath("$.result.content[0].artistId").value(singleArtist.getArtistId()))
			.andExpect(jsonPath("$.result.content[0].artistName").value("Single Artist"))
			.andExpect(jsonPath("$.result.content[0].occurrenceCount").value(2))
			.andExpect(jsonPath("$.result.content[0]", hasKey("popularitySnapshot")))
			.andExpect(jsonPath("$.result.content[0].isMajor").value(true))
			.andExpect(jsonPath("$.result.content[0].isExcluded").value(false))
			.andExpect(jsonPath("$.result.content[0].displayRank").value(2))
			.andExpect(jsonPath("$.result.page").value(0))
			.andExpect(jsonPath("$.result.size").value(20))
			.andExpect(jsonPath("$.result.totalElements").value(1))
			.andExpect(jsonPath("$.result.totalPages").value(1))
			.andExpect(jsonPath("$.result.hasNext").value(false));

		mockMvc.perform(get("/api/v1/analyses/{analysisId}/artists", analysis.getAnalysisId())
				.param("includeExcluded", "true")
				.param("page", "0")
				.param("size", "1")
				.param("sort", "displayRank,asc")
				.header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.content[0].artistId").value(repeatedArtist.getArtistId()))
			.andExpect(jsonPath("$.result.content[0].isExcluded").value(true))
			.andExpect(jsonPath("$.result.totalElements").value(2))
			.andExpect(jsonPath("$.result.totalPages").value(2))
			.andExpect(jsonPath("$.result.hasNext").value(true));

		String duplicateRequest = """
			{"artists":[
			  {"artistId":%d,"isExcluded":true},
			  {"artistId":%d,"isExcluded":false}
			]}
			""".formatted(repeatedArtist.getArtistId(), repeatedArtist.getArtistId());
		mockMvc.perform(patch("/api/v1/analyses/{analysisId}/artists", analysis.getAnalysisId())
				.header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(duplicateRequest))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value(2004));

		mockMvc.perform(patch("/api/v1/analyses/{analysisId}/artists", analysis.getAnalysisId())
				.header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"artists\":[{\"artistId\":999999,\"isExcluded\":true}]}"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value(2003));
	}

	@Test
	void validatesAuthenticationPlaylistIdAndOwnershipWhenAnalyzing() throws Exception {
		mockMvc.perform(post("/api/v1/playlists/1/analysis"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value(2001));

		User user = userRepository.saveAndFlush(User.createActive(LocalDateTime.now()));
		String authorization = bearerToken(user.getUserId());
		mockMvc.perform(post("/api/v1/playlists/0/analysis")
				.header(HttpHeaders.AUTHORIZATION, authorization))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(2000));

		mockMvc.perform(post("/api/v1/playlists/999999/analysis")
				.header(HttpHeaders.AUTHORIZATION, authorization))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value(2003));

		mockMvc.perform(get("/api/v1/playlists/1/analysis"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value(2001));

		mockMvc.perform(get("/api/v1/playlists/0/analysis")
				.header(HttpHeaders.AUTHORIZATION, authorization))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(2000));

		mockMvc.perform(get("/api/v1/playlists/999999/analysis")
				.header(HttpHeaders.AUTHORIZATION, authorization))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value(2003));

		mockMvc.perform(patch("/api/v1/analyses/1/artists")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"artists\":[{\"artistId\":1,\"isExcluded\":true}]}"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value(2001));

		mockMvc.perform(patch("/api/v1/analyses/0/artists")
				.header(HttpHeaders.AUTHORIZATION, authorization)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"artists\":[{\"artistId\":1,\"isExcluded\":true}]}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(2000));

		mockMvc.perform(patch("/api/v1/analyses/999999/artists")
				.header(HttpHeaders.AUTHORIZATION, authorization)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"artists\":[{\"artistId\":1,\"isExcluded\":true}]}"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value(2003));

		mockMvc.perform(get("/api/v1/analyses/1/artists"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value(2001));

		mockMvc.perform(get("/api/v1/analyses/0/artists")
				.header(HttpHeaders.AUTHORIZATION, authorization))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(2000));

		mockMvc.perform(get("/api/v1/analyses/999999/artists")
				.header(HttpHeaders.AUTHORIZATION, authorization))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value(2003));

		mockMvc.perform(get("/api/v1/analyses/1/artists")
				.param("sort", "unknown,asc")
				.header(HttpHeaders.AUTHORIZATION, authorization))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(2000));
	}

	private String bearerToken(Long userId) {
		return "Bearer " + accessTokenProvider.issue(userId);
	}
}
