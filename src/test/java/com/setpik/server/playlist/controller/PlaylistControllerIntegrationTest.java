package com.setpik.server.playlist.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.setpik.server.auth.client.SpotifyOAuthClient;
import com.setpik.server.auth.client.SpotifyApiException;
import com.setpik.server.auth.client.dto.SpotifyTokenResponse;
import com.setpik.server.auth.security.JwtAccessTokenProvider;
import com.setpik.server.auth.security.TokenCipher;
import com.setpik.server.member.domain.User;
import com.setpik.server.member.repository.UserRepository;
import com.setpik.server.playlist.client.SpotifyPlaylistClient;
import com.setpik.server.playlist.client.SpotifyPlaylistApiException;
import com.setpik.server.playlist.client.dto.SpotifyPlaylistSnapshot;
import com.setpik.server.playlist.client.dto.SpotifyArtistSnapshot;
import com.setpik.server.playlist.client.dto.SpotifyTrackSnapshot;
import com.setpik.server.playlist.domain.SpotifyPlaylist;
import com.setpik.server.playlist.repository.SpotifyPlaylistRepository;
import com.setpik.server.spotify.domain.SpotifyAccount;
import com.setpik.server.spotify.repository.SpotifyAccountRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PlaylistControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private SpotifyAccountRepository spotifyAccountRepository;

	@Autowired
	private SpotifyPlaylistRepository playlistRepository;

	@Autowired
	private JwtAccessTokenProvider accessTokenProvider;

	@Autowired
	private TokenCipher tokenCipher;

	@MockitoBean
	private SpotifyPlaylistClient spotifyPlaylistClient;

	@MockitoBean
	private SpotifyOAuthClient spotifyOAuthClient;

	@Test
	void syncsSpotifyDataForAuthenticatedUser() throws Exception {
		LocalDateTime now = LocalDateTime.now();
		User user = userRepository.saveAndFlush(User.createActive(now));
		spotifyAccountRepository.saveAndFlush(SpotifyAccount.connect(
			"spotify-user", "user@example.com", "setpik-user", null,
			tokenCipher.encrypt("spotify-access-token"),
			tokenCipher.encrypt("spotify-refresh-token"),
			now.plusHours(1), user.getUserId(), now
		));
		when(spotifyPlaylistClient.fetchMyPlaylists(anyString())).thenReturn(List.of(
			new SpotifyPlaylistSnapshot(
				"spotify-playlist-1", "My Playlist", "description", null,
				false, "spotify-user", "snapshot-1",
				List.of(new SpotifyTrackSnapshot(
					"spotify-track-1", "Track", "Album", null,
					"https://open.spotify.com/track/spotify-track-1", null,
					180000, true, now,
					List.of(
						new SpotifyArtistSnapshot(
							"spotify-artist-1", "Artist A",
							"https://open.spotify.com/artist/spotify-artist-1"
						),
						new SpotifyArtistSnapshot(
							"spotify-artist-2", "Artist B",
							"https://open.spotify.com/artist/spotify-artist-2"
						)
					)
				))
			)
		));

		mockMvc.perform(post("/api/v1/playlists/sync")
				.header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.code").value(1000))
			.andExpect(jsonPath("$.message").value("플레이리스트 동기화가 완료되었습니다."))
			.andExpect(jsonPath("$.result.syncedPlaylistCount").value(1))
			.andExpect(jsonPath("$.result.syncedTrackCount").value(1))
			.andExpect(jsonPath("$.result.lastSyncedAt", endsWith("+09:00")));

		SpotifyPlaylist syncedPlaylist = playlistRepository
			.findByUserIdAndSpotifyPlaylistId(user.getUserId(), "spotify-playlist-1")
			.orElseThrow();

		mockMvc.perform(get("/api/v1/playlists/{playlistId}/tracks", syncedPlaylist.getPlaylistId())
				.param("page", "0")
				.param("size", "20")
				.param("sort", "trackPosition,asc")
				.header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(1000))
			.andExpect(jsonPath("$.result.content.length()").value(1))
			.andExpect(jsonPath("$.result.content[0].playlistTrackId").isNumber())
			.andExpect(jsonPath("$.result.content[0].trackPosition").value(1))
			.andExpect(jsonPath("$.result.content[0].trackName").value("Track"))
			.andExpect(jsonPath("$.result.content[0].spotifyTrackId")
				.value("spotify-track-1"))
			.andExpect(jsonPath("$.result.content[0].albumImageUrl").value(nullValue()))
			.andExpect(jsonPath("$.result.content[0].artists.length()").value(2))
			.andExpect(jsonPath("$.result.content[0].artists[0].artistId").isNumber())
			.andExpect(jsonPath("$.result.content[0].artists[0].artistName").value("Artist A"))
			.andExpect(jsonPath("$.result.content[0].artists[1].artistName").value("Artist B"))
			.andExpect(jsonPath("$.result.page").value(0))
			.andExpect(jsonPath("$.result.size").value(20))
			.andExpect(jsonPath("$.result.totalElements").value(1))
			.andExpect(jsonPath("$.result.totalPages").value(1))
			.andExpect(jsonPath("$.result.hasNext").value(false));
	}

	@Test
	void rejectsSyncWithoutBearerToken() throws Exception {
		mockMvc.perform(post("/api/v1/playlists/sync"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value(2001));
	}

	@Test
	void returnsFilteredAndPagedPlaylistsUsingRequestedSort() throws Exception {
		LocalDateTime now = LocalDateTime.now();
		User user = userRepository.saveAndFlush(User.createActive(now));
		playlistRepository.saveAllAndFlush(List.of(
			new SpotifyPlaylist(
				"spotify-playlist-b", "Festival Z", null, "https://image/z",
				false, "spotify-user", "snapshot-z", 20, user.getUserId()
			),
			new SpotifyPlaylist(
				"spotify-playlist-a", "Festival A", null, "https://image/a",
				false, "spotify-user", "snapshot-a", 10, user.getUserId()
			),
			new SpotifyPlaylist(
				"spotify-playlist-c", "Daily Mix", null, "https://image/c",
				false, "spotify-user", "snapshot-c", 30, user.getUserId()
			)
		));

		mockMvc.perform(get("/api/v1/playlists")
				.param("page", "0")
				.param("size", "1")
				.param("sort", "playlistName,asc")
				.param("keyword", "festival")
				.header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(1000))
			.andExpect(jsonPath("$.result.content.length()").value(1))
			.andExpect(jsonPath("$.result.content[0].spotifyPlaylistId")
				.value("spotify-playlist-a"))
			.andExpect(jsonPath("$.result.content[0].playlistName").value("Festival A"))
			.andExpect(jsonPath("$.result.content[0].trackCount").value(10))
			.andExpect(jsonPath("$.result.content[0].lastSyncedAt", endsWith("+09:00")))
			.andExpect(jsonPath("$.result.page").value(0))
			.andExpect(jsonPath("$.result.size").value(1))
			.andExpect(jsonPath("$.result.totalElements").value(2))
			.andExpect(jsonPath("$.result.totalPages").value(2))
			.andExpect(jsonPath("$.result.hasNext").value(true));
	}

	@Test
	void returnsInvalidRequestForInvalidPlaylistPagingOrSort() throws Exception {
		User user = userRepository.saveAndFlush(User.createActive(LocalDateTime.now()));

		mockMvc.perform(get("/api/v1/playlists")
				.param("size", "101")
				.header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(2000));

		mockMvc.perform(get("/api/v1/playlists")
				.param("sort", "unknown,asc")
				.header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(2000));
	}

	@Test
	void returnsOwnedPlaylistDetailUsingSpecifiedResponseFields() throws Exception {
		LocalDateTime now = LocalDateTime.now();
		User user = userRepository.saveAndFlush(User.createActive(now));
		SpotifyPlaylist playlist = playlistRepository.saveAndFlush(new SpotifyPlaylist(
			"spotify-playlist-detail", "Festival Playlist", "SetPIK analysis playlist",
			"https://image/cover", false, "spotify-owner", "snapshot-detail", 42,
			user.getUserId()
		));

		mockMvc.perform(get("/api/v1/playlists/{playlistId}", playlist.getPlaylistId())
				.header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(1000))
			.andExpect(jsonPath("$.result.playlistId").value(playlist.getPlaylistId()))
			.andExpect(jsonPath("$.result.spotifyPlaylistId").value("spotify-playlist-detail"))
			.andExpect(jsonPath("$.result.playlistName").value("Festival Playlist"))
			.andExpect(jsonPath("$.result.description").value("SetPIK analysis playlist"))
			.andExpect(jsonPath("$.result.trackCount").value(42))
			.andExpect(jsonPath("$.result.isPublic").value(false))
			.andExpect(jsonPath("$.result.ownerSpotifyUserId").value("spotify-owner"))
			.andExpect(jsonPath("$.result.analysisAvailable").value(true))
			.andExpect(jsonPath("$.result.lastSyncedAt", endsWith("+09:00")))
			.andExpect(jsonPath("$.result.deletedAt").value(nullValue()))
			.andExpect(jsonPath("$.result.coverImageUrl").doesNotExist());
	}

	@Test
	void validatesPlaylistIdAndHidesAnotherUsersPlaylist() throws Exception {
		LocalDateTime now = LocalDateTime.now();
		User owner = userRepository.saveAndFlush(User.createActive(now));
		User requester = userRepository.saveAndFlush(User.createActive(now));
		SpotifyPlaylist playlist = playlistRepository.saveAndFlush(new SpotifyPlaylist(
			"spotify-playlist-private", "Private Playlist", null, null, false,
			"spotify-owner", "snapshot-private", 1, owner.getUserId()
		));
		String authorization = bearerToken(requester.getUserId());

		mockMvc.perform(get("/api/v1/playlists/0")
				.header(HttpHeaders.AUTHORIZATION, authorization))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(2000));

		mockMvc.perform(get("/api/v1/playlists/{playlistId}", playlist.getPlaylistId())
				.header(HttpHeaders.AUTHORIZATION, authorization))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value(2003));

		mockMvc.perform(get("/api/v1/playlists/999999")
				.header(HttpHeaders.AUTHORIZATION, authorization))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value(2003));

		mockMvc.perform(get("/api/v1/playlists/0/tracks")
				.header(HttpHeaders.AUTHORIZATION, authorization))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(2000));

		mockMvc.perform(get("/api/v1/playlists/{playlistId}/tracks", playlist.getPlaylistId())
				.header(HttpHeaders.AUTHORIZATION, authorization))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value(2003));

		String ownerAuthorization = bearerToken(owner.getUserId());
		mockMvc.perform(get("/api/v1/playlists/{playlistId}/tracks", playlist.getPlaylistId())
				.param("size", "101")
				.header(HttpHeaders.AUTHORIZATION, ownerAuthorization))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(2000));

		mockMvc.perform(get("/api/v1/playlists/{playlistId}/tracks", playlist.getPlaylistId())
				.param("sort", "unknown,asc")
				.header(HttpHeaders.AUTHORIZATION, ownerAuthorization))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(2000));
	}

	@Test
	void returnsSpotifyConnectionRequiredWhenAccountDoesNotExist() throws Exception {
		User user = userRepository.saveAndFlush(User.createActive(LocalDateTime.now()));

		mockMvc.perform(post("/api/v1/playlists/sync")
				.header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value(2100));
	}

	@Test
	void returnsReauthenticationRequiredWhenSpotifyRefreshFails() throws Exception {
		LocalDateTime now = LocalDateTime.now();
		User user = userRepository.saveAndFlush(User.createActive(now));
		spotifyAccountRepository.saveAndFlush(SpotifyAccount.connect(
			"spotify-user-reauth", "reauth@example.com", "reauth-user", null,
			tokenCipher.encrypt("expired-access-token"),
			tokenCipher.encrypt("invalid-refresh-token"),
			now.minusMinutes(1), user.getUserId(), now.minusHours(1)
		));
		when(spotifyOAuthClient.refreshAccessToken("invalid-refresh-token"))
			.thenThrow(new SpotifyApiException("refresh failed"));

		mockMvc.perform(post("/api/v1/playlists/sync")
				.header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value(2101));
	}

	@Test
	void returnsSpotifyApiErrorWhenPlaylistRequestFails() throws Exception {
		LocalDateTime now = LocalDateTime.now();
		User user = userRepository.saveAndFlush(User.createActive(now));
		spotifyAccountRepository.saveAndFlush(SpotifyAccount.connect(
			"spotify-user-api-error", "api-error@example.com", "api-error-user", null,
			tokenCipher.encrypt("spotify-access-token"),
			tokenCipher.encrypt("spotify-refresh-token"),
			now.plusHours(1), user.getUserId(), now
		));
		when(spotifyPlaylistClient.fetchMyPlaylists("spotify-access-token"))
			.thenThrow(new SpotifyPlaylistApiException("Spotify API failed"));

		mockMvc.perform(post("/api/v1/playlists/sync")
				.header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
			.andExpect(status().isBadGateway())
			.andExpect(jsonPath("$.code").value(2200));
	}

	@Test
	void refreshesExpiredSpotifyTokenBeforeSync() throws Exception {
		LocalDateTime now = LocalDateTime.now();
		User user = userRepository.saveAndFlush(User.createActive(now));
		SpotifyAccount account = spotifyAccountRepository.saveAndFlush(SpotifyAccount.connect(
			"spotify-user-expired", "expired@example.com", "expired-user", null,
			tokenCipher.encrypt("expired-access-token"),
			tokenCipher.encrypt("spotify-refresh-token"),
			now.minusMinutes(1), user.getUserId(), now.minusHours(1)
		));
		when(spotifyOAuthClient.refreshAccessToken("spotify-refresh-token"))
			.thenReturn(new SpotifyTokenResponse(
				"renewed-access-token", "Bearer", "playlist-read-private", 3600, null
			));
		when(spotifyPlaylistClient.fetchMyPlaylists("renewed-access-token"))
			.thenReturn(List.of());

		mockMvc.perform(post("/api/v1/playlists/sync")
				.header(HttpHeaders.AUTHORIZATION, bearerToken(user.getUserId())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.syncedPlaylistCount").value(0));

		verify(spotifyOAuthClient).refreshAccessToken("spotify-refresh-token");
		SpotifyAccount refreshed = spotifyAccountRepository
			.findById(account.getSpotifyAccountId())
			.orElseThrow();
		assertThat(tokenCipher.decrypt(refreshed.getAccessTokenEncrypted()))
			.isEqualTo("renewed-access-token");
		assertThat(refreshed.getTokenExpiresAt()).isAfter(now.plusMinutes(59));
	}

	private String bearerToken(Long userId) {
		return "Bearer " + accessTokenProvider.issue(userId);
	}
}
