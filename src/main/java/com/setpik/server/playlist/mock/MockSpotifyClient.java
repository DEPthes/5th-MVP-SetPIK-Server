package com.setpik.server.playlist.mock;

import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Spotify API 대신 고정된 응답을 돌려주는 임시 클라이언트.
 * TODO: 인증 기능 병합 후 실제 Spotify Web API 호출로 교체한다.
 */
@Component
public class MockSpotifyClient {

	public List<MockSpotifyPlaylist> fetchMyPlaylists(Long userId) {
		return List.of(
			new MockSpotifyPlaylist(
				"37i9dQZF1DX1BzuNZ2vsg1",
				"Indie Rock Favorites",
				"Mock playlist for local development",
				"https://i.scdn.co/image/mock-indie",
				true,
				"mock_spotify_user",
				"snapshot-indie-001",
				List.of(
					new MockSpotifyTrack(
						"3n3Ppam7vgaVa1iaRUc9Lp",
						"Desert Eagle",
						"Power Andre 99",
						"https://i.scdn.co/image/mock-album-1",
						"https://open.spotify.com/track/3n3Ppam7vgaVa1iaRUc9Lp",
						null,
						215000,
						true
					),
					new MockSpotifyTrack(
						"7ouMYWpwJ422jRcDASZB7P",
						"Nice to Meet You",
						"Nonadaptation",
						"https://i.scdn.co/image/mock-album-2",
						"https://open.spotify.com/track/7ouMYWpwJ422jRcDASZB7P",
						null,
						198000,
						true
					)
				)
			),
			new MockSpotifyPlaylist(
				"37i9dQZF1DX2sUQwD7tbmL",
				"Chill Drive",
				null,
				"https://i.scdn.co/image/mock-chill",
				false,
				"mock_spotify_user",
				"snapshot-chill-001",
				List.of(
					new MockSpotifyTrack(
						"1301WleyT98MSxVHPZCA6M",
						"seasons",
						"summer flame",
						"https://i.scdn.co/image/mock-album-3",
						"https://open.spotify.com/track/1301WleyT98MSxVHPZCA6M",
						null,
						243000,
						true
					)
				)
			)
		);
	}
}
