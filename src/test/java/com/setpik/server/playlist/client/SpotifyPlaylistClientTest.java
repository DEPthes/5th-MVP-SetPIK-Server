package com.setpik.server.playlist.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.setpik.server.playlist.client.dto.SpotifyPlaylistSnapshot;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class SpotifyPlaylistClientTest {

	@Test
	void fetchesAndMapsCurrentSpotifyPlaylistItemsResponse() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		SpotifyPlaylistClient client = new SpotifyPlaylistClient(builder);

		server.expect(requestTo("https://api.spotify.com/v1/me/playlists?limit=50&offset=0"))
			.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
			.andRespond(withSuccess("""
				{
				  "items": [{
				    "id": "playlist-1",
				    "name": "My Playlist",
				    "description": "description",
				    "images": [{"url": "https://image/playlist"}],
				    "public": false,
				    "owner": {"id": "spotify-user"},
				    "snapshot_id": "snapshot-1"
				  }],
				  "next": null
				}
				""", MediaType.APPLICATION_JSON));
		server.expect(requestTo("https://api.spotify.com/v1/playlists/playlist-1/items?limit=50&offset=0"))
			.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
			.andRespond(withSuccess("""
				{
				  "items": [{
				    "added_at": "2026-08-07T01:00:00Z",
				    "item": {
				      "id": "track-1",
				      "name": "Track",
				      "type": "track",
				      "artists": [
				        {
				          "id": "artist-1",
				          "name": "Artist A",
				          "external_urls": {"spotify": "https://open.spotify.com/artist/artist-1"}
				        }
				      ],
				      "album": {"name": "Album", "images": [{"url": "https://image/album"}]},
				      "external_urls": {"spotify": "https://open.spotify.com/track/track-1"},
				      "duration_ms": 180000,
				      "is_playable": true,
				      "is_local": false
				    }
				  }],
				  "next": null
				}
				""", MediaType.APPLICATION_JSON));

		List<SpotifyPlaylistSnapshot> result = client.fetchMyPlaylists("access-token");

		assertThat(result).hasSize(1);
		assertThat(result.get(0).spotifyPlaylistId()).isEqualTo("playlist-1");
		assertThat(result.get(0).tracks()).hasSize(1);
		assertThat(result.get(0).tracks().get(0).spotifyTrackId()).isEqualTo("track-1");
		assertThat(result.get(0).tracks().get(0).albumName()).isEqualTo("Album");
		assertThat(result.get(0).tracks().get(0).artists()).hasSize(1);
		assertThat(result.get(0).tracks().get(0).artists().get(0).artistName())
			.isEqualTo("Artist A");
		server.verify();
	}
}
