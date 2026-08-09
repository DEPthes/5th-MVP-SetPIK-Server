package com.setpik.server.playlist.client;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.setpik.server.playlist.client.dto.SpotifyPlaylistSnapshot;
import com.setpik.server.playlist.client.dto.SpotifyArtistSnapshot;
import com.setpik.server.playlist.client.dto.SpotifyTrackSnapshot;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class SpotifyPlaylistClient {

	private static final Logger log = LoggerFactory.getLogger(SpotifyPlaylistClient.class);
	private static final String API_BASE_URI = "https://api.spotify.com/v1";
	private static final int PAGE_SIZE = 50;
	private final RestClient restClient;

	public SpotifyPlaylistClient(RestClient.Builder restClientBuilder) {
		this.restClient = restClientBuilder.build();
	}

	/** 현재 Spotify 사용자의 모든 플레이리스트와 트랙 페이지를 순회한다. */
	public List<SpotifyPlaylistSnapshot> fetchMyPlaylists(String accessToken) {
		try {
			List<SpotifyPlaylistSnapshot> result = new ArrayList<>();
			int offset = 0;
			while (true) {
				PlaylistPage page = getPlaylistPage(accessToken, offset);
				for (PlaylistItem playlist : page.safeItems()) {
					if (playlist.id() != null && !playlist.id().isBlank()) {
						result.add(toSnapshot(accessToken, playlist));
					}
				}
				if (page.next() == null || page.next().isBlank() || page.safeItems().isEmpty()) {
					break;
				}
				offset += page.safeItems().size();
			}
			return result;
		} catch (RestClientResponseException exception) {
			logSpotifyError(exception);
			throw new SpotifyPlaylistApiException("Spotify 플레이리스트 조회에 실패했습니다.", exception);
		} catch (RestClientException exception) {
			throw new SpotifyPlaylistApiException("Spotify 플레이리스트 조회에 실패했습니다.", exception);
		}
	}

	/** Spotify에 새 플레이리스트를 만들고 플레이리스트 ID를 반환한다. */
	public String createPlaylist(String accessToken, String spotifyUserId, String title, boolean isPublic) {
		try {
			CreatePlaylistRequest request = new CreatePlaylistRequest(title, isPublic);
			CreatedPlaylist created = restClient.post()
				.uri(API_BASE_URI + "/users/" + spotifyUserId + "/playlists")
				.headers(headers -> headers.setBearerAuth(accessToken))
				.body(request)
				.retrieve()
				.body(CreatedPlaylist.class);
			if (created == null || created.id() == null) {
				throw new SpotifyPlaylistApiException("Spotify 플레이리스트 생성 응답이 비어 있습니다.");
			}
			return created.id();
		} catch (RestClientResponseException exception) {
			logSpotifyError(exception);
			throw new SpotifyPlaylistApiException("Spotify 플레이리스트 생성에 실패했습니다.", exception);
		} catch (RestClientException exception) {
			throw new SpotifyPlaylistApiException("Spotify 플레이리스트 생성에 실패했습니다.", exception);
		}
	}

	/** 생성된 Spotify 플레이리스트에 트랙을 추가한다. */
	public void addTracks(String accessToken, String spotifyPlaylistId, List<String> spotifyTrackIds) {
		if (spotifyTrackIds == null || spotifyTrackIds.isEmpty()) {
			return;
		}
		try {
			List<String> uris = spotifyTrackIds.stream().map(id -> "spotify:track:" + id).toList();
			AddTracksRequest request = new AddTracksRequest(uris);
			restClient.post()
				.uri(API_BASE_URI + "/playlists/" + spotifyPlaylistId + "/tracks")
				.headers(headers -> headers.setBearerAuth(accessToken))
				.body(request)
				.retrieve()
				.toBodilessEntity();
		} catch (RestClientResponseException exception) {
			logSpotifyError(exception);
			throw new SpotifyPlaylistApiException("Spotify 트랙 추가에 실패했습니다.", exception);
		} catch (RestClientException exception) {
			throw new SpotifyPlaylistApiException("Spotify 트랙 추가에 실패했습니다.", exception);
		}
	}

	/** 매칭된 아티스트의 Spotify Top Tracks 중 대표곡 1곡을 조회한다. 실패 시 null을 반환한다. */
	public SpotifyTrackSnapshot fetchTopTrack(String accessToken, String spotifyArtistId) {
		try {
			TopTracksResponse response = restClient.get()
				.uri(API_BASE_URI + "/artists/" + spotifyArtistId + "/top-tracks?market=KR")
				.headers(headers -> headers.setBearerAuth(accessToken))
				.retrieve()
				.body(TopTracksResponse.class);
			if (response == null || response.safeTracks().isEmpty()) {
				return null;
			}
			return toTrackSnapshot(response.safeTracks().get(0));
		} catch (RestClientResponseException exception) {
			logSpotifyError(exception);
			return null;
		} catch (RestClientException exception) {
			return null;
		}
	}

	private PlaylistPage getPlaylistPage(String accessToken, int offset) {
		PlaylistPage page = restClient.get()
			.uri(API_BASE_URI + "/me/playlists?limit=" + PAGE_SIZE + "&offset=" + offset)
			.headers(headers -> headers.setBearerAuth(accessToken))
			.retrieve()
			.body(PlaylistPage.class);
		if (page == null) {
			throw new SpotifyPlaylistApiException("Spotify 플레이리스트 응답이 비어 있습니다.");
		}
		return page;
	}

	private SpotifyPlaylistSnapshot toSnapshot(String accessToken, PlaylistItem playlist) {
		List<SpotifyTrackSnapshot> tracks = fetchPlaylistTracks(accessToken, playlist.id());
		return new SpotifyPlaylistSnapshot(
			playlist.id(),
			playlist.name(),
			playlist.description(),
			firstImageUrl(playlist.images()),
			playlist.isPublic(),
			playlist.owner() == null ? null : playlist.owner().id(),
			playlist.snapshotId(),
			tracks
		);
	}

	private List<SpotifyTrackSnapshot> fetchPlaylistTracks(String accessToken, String playlistId) {
		List<SpotifyTrackSnapshot> result = new ArrayList<>();
		int offset = 0;
		while (true) {
			PlaylistItemsPage page = restClient.get()
				.uri(API_BASE_URI + "/playlists/" + playlistId + "/items?limit=" + PAGE_SIZE
					+ "&offset=" + offset)
				.headers(headers -> headers.setBearerAuth(accessToken))
				.retrieve()
				.body(PlaylistItemsPage.class);
			if (page == null) {
				throw new SpotifyPlaylistApiException("Spotify 트랙 응답이 비어 있습니다.");
			}

			page.safeItems().stream()
				.map(PlaylistEntry::toSnapshot)
				.filter(java.util.Objects::nonNull)
				.forEach(result::add);
			if (page.next() == null || page.next().isBlank() || page.safeItems().isEmpty()) {
				break;
			}
			offset += page.safeItems().size();
		}
		return result;
	}

	private String firstImageUrl(List<Image> images) {
		return images == null || images.isEmpty() ? null : images.get(0).url();
	}

	private SpotifyTrackSnapshot toTrackSnapshot(TrackItem item) {
		return new SpotifyTrackSnapshot(
			item.id(),
			item.name(),
			item.album() == null ? null : item.album().name(),
			item.album() == null ? null : firstImageUrl(item.album().images()),
			item.externalUrls() == null ? null : item.externalUrls().spotify(),
			item.previewUrl(),
			item.durationMs(),
			Boolean.TRUE.equals(item.isPlayable()),
			null,
			item.safeArtists().stream()
				.filter(artist -> artist.id() != null && artist.name() != null)
				.map(artist -> new SpotifyArtistSnapshot(
					artist.id(), artist.name(),
					artist.externalUrls() == null ? null : artist.externalUrls().spotify()
				))
				.toList()
		);
	}

	private void logSpotifyError(RestClientResponseException exception) {
		String body = exception.getResponseBodyAsString();
		if (body.length() > 500) {
			body = body.substring(0, 500);
		}
		log.warn("Spotify API 호출 실패: status={}, response={}",
			exception.getStatusCode().value(), body);
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record PlaylistPage(List<PlaylistItem> items, String next) {
		private List<PlaylistItem> safeItems() {
			return items == null ? List.of() : items;
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record PlaylistItem(
		String id,
		String name,
		String description,
		List<Image> images,
		@JsonProperty("public") Boolean isPublic,
		Owner owner,
		@JsonProperty("snapshot_id") String snapshotId
	) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record PlaylistItemsPage(List<PlaylistEntry> items, String next) {
		private List<PlaylistEntry> safeItems() {
			return items == null ? List.of() : items;
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record PlaylistEntry(
		@JsonAlias("track") TrackItem item,
		@JsonProperty("added_at") OffsetDateTime addedAt
	) {
		private SpotifyTrackSnapshot toSnapshot() {
			if (item == null || item.id() == null || !"track".equals(item.type()) || item.isLocal()) {
				return null;
			}
			return new SpotifyTrackSnapshot(
				item.id(),
				item.name(),
				item.album() == null ? null : item.album().name(),
				item.album() == null ? null : firstAlbumImage(item.album().images()),
				item.externalUrls() == null ? null : item.externalUrls().spotify(),
				item.previewUrl(),
				item.durationMs(),
				Boolean.TRUE.equals(item.isPlayable()),
				addedAt == null ? null : addedAt.toLocalDateTime(),
				item.safeArtists().stream()
					.filter(artist -> artist.id() != null && artist.name() != null)
					.map(artist -> new SpotifyArtistSnapshot(
						artist.id(),
						artist.name(),
						artist.externalUrls() == null ? null : artist.externalUrls().spotify()
					))
					.toList()
			);
		}

		private static String firstAlbumImage(List<Image> images) {
			return images == null || images.isEmpty() ? null : images.get(0).url();
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record TrackItem(
		String id,
		String name,
		String type,
		Album album,
		List<ArtistItem> artists,
		@JsonProperty("external_urls") ExternalUrls externalUrls,
		@JsonProperty("preview_url") String previewUrl,
		@JsonProperty("duration_ms") Integer durationMs,
		@JsonProperty("is_playable") Boolean isPlayable,
		@JsonProperty("is_local") boolean isLocal
	) {
		private List<ArtistItem> safeArtists() {
			return artists == null ? List.of() : artists;
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record ArtistItem(
		String id,
		String name,
		@JsonProperty("external_urls") ExternalUrls externalUrls
	) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record Album(String name, List<Image> images) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record Image(String url) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record Owner(String id) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record ExternalUrls(String spotify) {
	}

	private record CreatePlaylistRequest(String name, @JsonProperty("public") boolean isPublic) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record CreatedPlaylist(String id) {
	}

	private record AddTracksRequest(List<String> uris) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record TopTracksResponse(List<TrackItem> tracks) {
		private List<TrackItem> safeTracks() {
			return tracks == null ? List.of() : tracks;
		}
	}
}