package com.setpik.server.playlist.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.setpik.server.playlist.client.dto.SpotifyPlaylistSnapshot;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class SpotifyPlaylistClientTest {

	@Test
	void createsPlaylistAndAddsItemsUsingCurrentSpotifyEndpoints() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		SpotifyPlaylistClient client = new SpotifyPlaylistClient(builder);

		server.expect(requestTo("https://api.spotify.com/v1/me/playlists"))
			.andExpect(method(HttpMethod.POST))
			.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
			.andExpect(content().json("{\"name\":\"Prestudy\",\"public\":false}"))
			.andRespond(withSuccess("{\"id\":\"playlist-created\"}", MediaType.APPLICATION_JSON));
		server.expect(requestTo("https://api.spotify.com/v1/playlists/playlist-created/items"))
			.andExpect(method(HttpMethod.POST))
			.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
			.andExpect(content().json("{\"uris\":[\"spotify:track:track-1\"]}"))
			.andRespond(withSuccess("{\"snapshot_id\":\"snapshot\"}", MediaType.APPLICATION_JSON));

		String playlistId = client.createPlaylist("access-token", "Prestudy", false);
		client.addTracks("access-token", playlistId, List.of("track-1"));

		assertThat(playlistId).isEqualTo("playlist-created");
		server.verify();
	}

	@Test
	void removesPlaylistFromCurrentUsersLibrary() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		SpotifyPlaylistClient client = new SpotifyPlaylistClient(builder);

		server.expect(requestTo(
			"https://api.spotify.com/v1/me/library?uris=spotify:playlist:playlist-created"))
			.andExpect(method(HttpMethod.DELETE))
			.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
			.andRespond(withSuccess());

		client.removePlaylistFromLibrary("access-token", "playlist-created");

		server.verify();
	}

	@Test
	void collectsRepresentativeTracksFromArtistsAlbums() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		SpotifyPlaylistClient client = new SpotifyPlaylistClient(builder);

		server.expect(requestTo(containsString("/artists/artist-1/albums?")))
			.andExpect(requestTo(containsString("include_groups=album,single")))
			.andExpect(requestTo(containsString("offset=0")))
			.andExpect(method(HttpMethod.GET))
			.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
			.andRespond(withSuccess(albumPageResponse("album-1", null),
				MediaType.APPLICATION_JSON));
		server.expect(requestTo(containsString("/albums?ids=album-1")))
			.andRespond(withSuccess("""
				{
				  "albums": [{
				    "id": "album-1", "name": "Album", "images": [],
				    "tracks": {"items": [{
				      "id": "track-1", "name": "Song A", "type": "track",
				      "artists": [{"id": "artist-1", "name": "Artist A"}],
				      "duration_ms": 180000, "is_playable": true, "is_local": false
				    }, {
				      "id": "track-2", "name": "Song B", "type": "track",
				      "artists": [{"id": "artist-1", "name": "Artist A"}],
				      "duration_ms": 190000, "is_playable": true, "is_local": false
				    }, {
				      "id": "other-track", "name": "Wrong Artist Song", "type": "track",
				      "artists": [{"id": "artist-2", "name": "Artist B"}],
				      "duration_ms": 200000, "is_playable": true, "is_local": false
				    }]}
				  }]
				}
				""", MediaType.APPLICATION_JSON));

		var result = client.fetchRepresentativeTracks(
			"access-token", "artist-1", 20);

		assertThat(result)
			.extracting(track -> track.spotifyTrackId())
			.containsExactly("track-1", "track-2");
		server.verify();
	}

	@Test
	void searchesFollowingAlbumPagesUntilRepresentativeTrackLimitIsReached() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		SpotifyPlaylistClient client = new SpotifyPlaylistClient(builder);

		server.expect(requestTo(containsString("/artists/artist-1/albums?")))
			.andExpect(requestTo(containsString("offset=0")))
			.andRespond(withSuccess(albumPageResponse(
				"album-1", "https://api.spotify.com/v1/artists/artist-1/albums?offset=1"),
				MediaType.APPLICATION_JSON));
		server.expect(requestTo(containsString("/albums?ids=album-1")))
			.andRespond(withSuccess(albumResponse("album-1", "wrong-track", "artist-2"),
				MediaType.APPLICATION_JSON));
		server.expect(requestTo(containsString("/artists/artist-1/albums?")))
			.andExpect(requestTo(containsString("offset=1")))
			.andRespond(withSuccess(albumPageResponse("album-2", null),
				MediaType.APPLICATION_JSON));
		server.expect(requestTo(containsString("/albums?ids=album-2")))
			.andRespond(withSuccess(albumResponse("album-2", "track-1", "artist-1"),
				MediaType.APPLICATION_JSON));

		var result = client.fetchRepresentativeTracks(
			"access-token", "artist-1", 20);

		assertThat(result)
			.extracting(track -> track.spotifyTrackId())
			.containsExactly("track-1");
		server.verify();
	}

	private String albumPageResponse(String albumId, String next) {
		String nextValue = next == null ? "null" : "\"" + next + "\"";
		return """
			{
			  "items": [{"id": "%s"}],
			  "next": %s
			}
			""".formatted(albumId, nextValue);
	}

	private String albumResponse(String albumId, String trackId, String artistId) {
		return """
			{
			  "albums": [{
			    "id": "%s", "name": "Album", "images": [],
			    "tracks": {"items": [{
			      "id": "%s", "name": "Song", "type": "track",
			      "artists": [{"id": "%s", "name": "Artist"}],
			      "duration_ms": 180000, "is_playable": true, "is_local": false
			    }]}
			  }]
			}
			""".formatted(albumId, trackId, artistId);
	}

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
		server.expect(requestTo("https://api.spotify.com/v1/artists/artist-1"))
			.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
			.andRespond(withSuccess("""
				{
				  "id": "artist-1",
				  "name": "Artist A",
				  "images": [{"url": "https://image/artist"}],
				  "popularity": 87,
				  "external_urls": {"spotify": "https://open.spotify.com/artist/artist-1"}
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
		assertThat(result.get(0).tracks().get(0).artists().get(0).imageUrl())
			.isEqualTo("https://image/artist");
		assertThat(result.get(0).tracks().get(0).artists().get(0).popularity())
			.isEqualTo((short) 87);
		server.verify();
	}
}
