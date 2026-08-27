package com.setpik.server.playlist.client;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.setpik.server.playlist.client.dto.SpotifyPlaylistSnapshot;
import com.setpik.server.playlist.client.dto.SpotifyArtistSnapshot;
import com.setpik.server.playlist.client.dto.SpotifyTrackSnapshot;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

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
			return enrichArtistDetails(accessToken, result);
		} catch (RestClientResponseException exception) {
			logSpotifyError(exception);
			throw new SpotifyPlaylistApiException("Spotify 플레이리스트 조회에 실패했습니다.", exception);
		} catch (RestClientException exception) {
			throw new SpotifyPlaylistApiException("Spotify 플레이리스트 조회에 실패했습니다.", exception);
		}
	}

	/** 트랙 응답에 없는 아티스트 인기도와 이미지를 상세 API에서 보완한다. */
	private List<SpotifyPlaylistSnapshot> enrichArtistDetails(
		String accessToken,
		List<SpotifyPlaylistSnapshot> playlists
	) {
		Set<String> artistIds = playlists.stream()
			.flatMap(playlist -> playlist.tracks().stream())
			.flatMap(track -> track.artists().stream())
			.map(SpotifyArtistSnapshot::spotifyArtistId)
			.filter(id -> id != null && !id.isBlank())
			.collect(Collectors.toCollection(LinkedHashSet::new));

		Map<String, SpotifyArtistSnapshot> detailsById = artistIds.stream()
			.map(id -> fetchArtistDetail(accessToken, id))
			.filter(java.util.Objects::nonNull)
			.collect(Collectors.toMap(
				SpotifyArtistSnapshot::spotifyArtistId,
				Function.identity()
			));

		return playlists.stream()
			.map(playlist -> new SpotifyPlaylistSnapshot(
				playlist.spotifyPlaylistId(),
				playlist.playlistName(),
				playlist.description(),
				playlist.coverImageUrl(),
				playlist.isPublic(),
				playlist.ownerSpotifyUserId(),
				playlist.snapshotId(),
				playlist.tracks().stream()
					.map(track -> enrichTrackArtists(track, detailsById))
					.toList()
			))
			.toList();
	}

	private SpotifyTrackSnapshot enrichTrackArtists(
		SpotifyTrackSnapshot track,
		Map<String, SpotifyArtistSnapshot> detailsById
	) {
		return new SpotifyTrackSnapshot(
			track.spotifyTrackId(),
			track.trackName(),
			track.albumName(),
			track.albumImageUrl(),
			track.spotifyTrackUrl(),
			track.previewUrl(),
			track.durationMs(),
			track.isPlayable(),
			track.addedAt(),
			track.artists().stream()
				.map(artist -> detailsById.getOrDefault(artist.spotifyArtistId(), artist))
				.toList()
		);
	}

	private SpotifyArtistSnapshot fetchArtistDetail(String accessToken, String artistId) {
		try {
			ArtistDetail detail = restClient.get()
				.uri(API_BASE_URI + "/artists/" + artistId)
				.headers(headers -> headers.setBearerAuth(accessToken))
				.retrieve()
				.body(ArtistDetail.class);
			if (detail == null || detail.id() == null || detail.name() == null) {
				log.warn("Spotify 아티스트 상세 응답이 비어 있습니다: requestedArtistId={}", artistId);
				return null;
			}
			if (detail.popularity() == null || detail.genres() == null || detail.genres().isEmpty()) {
				log.warn(
					"Spotify 아티스트 상세 응답 필드 누락: artistId={}, popularity={}, genres={}",
					detail.id(), detail.popularity(), detail.genres()
				);
			}
			return new SpotifyArtistSnapshot(
				detail.id(),
				detail.name(),
				detail.externalUrls() == null ? null : detail.externalUrls().spotify(),
				firstImageUrl(detail.images()),
				toPopularity(detail.popularity()),
				detail.genres()
			);
		} catch (RestClientResponseException exception) {
			logSpotifyError(exception);
			return null;
		} catch (RestClientException exception) {
			log.warn("Spotify 아티스트 상세 조회 실패: artistId={}", artistId);
			return null;
		}
	}

	private Short toPopularity(Integer popularity) {
		if (popularity == null || popularity < 0 || popularity > 100) {
			return null;
		}
		return popularity.shortValue();
	}

	/** Spotify에 새 플레이리스트를 만들고 플레이리스트 ID를 반환한다. */
	public String createPlaylist(String accessToken, String title, boolean isPublic) {
		try {
			CreatePlaylistRequest request = new CreatePlaylistRequest(title, isPublic);
			CreatedPlaylist created = restClient.post()
				.uri(API_BASE_URI + "/me/playlists")
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
				.uri(API_BASE_URI + "/playlists/" + spotifyPlaylistId + "/items")
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

	/** Spotify에서 플레이리스트를 영구 삭제할 수 없어 현재 사용자의 라이브러리에서 제거한다. */
	public void removePlaylistFromLibrary(String accessToken, String spotifyPlaylistId) {
		try {
			java.net.URI uri = UriComponentsBuilder.fromUriString(API_BASE_URI + "/me/library")
				.queryParam("uris", "spotify:playlist:" + spotifyPlaylistId)
				.build()
				.encode()
				.toUri();
			restClient.delete()
				.uri(uri)
				.headers(headers -> headers.setBearerAuth(accessToken))
				.retrieve()
				.toBodilessEntity();
		} catch (RestClientResponseException exception) {
			logSpotifyError(exception);
			throw new SpotifyPlaylistApiException("Spotify 플레이리스트 제거에 실패했습니다.", exception);
		} catch (RestClientException exception) {
			throw new SpotifyPlaylistApiException("Spotify 플레이리스트 제거에 실패했습니다.", exception);
		}
	}

	/** 매칭된 아티스트의 Spotify 검색 결과에서 대표곡을 최대 limit곡 조회한다. 실패 시 빈 목록을 반환한다. */
	public List<SpotifyTrackSnapshot> fetchRepresentativeTracks(
		String accessToken,
		String spotifyArtistId,
		String artistName,
		int limit
	) {
		try {
			java.net.URI uri = UriComponentsBuilder.fromUriString(API_BASE_URI + "/search")
				.queryParam("q", "artist:\"" + artistName + "\"")
				.queryParam("type", "track")
				.queryParam("market", "KR")
				.queryParam("limit", limit)
				.build()
				.encode()
				.toUri();
			TrackSearchResponse response = restClient.get()
				.uri(uri)
				.headers(headers -> headers.setBearerAuth(accessToken))
				.retrieve()
				.body(TrackSearchResponse.class);
			if (response == null || response.tracks() == null) {
				return List.of();
			}
			Map<String, SpotifyTrackSnapshot> uniqueTracks = response.tracks().safeItems().stream()
				.filter(item -> item.safeArtists().stream()
					.anyMatch(artist -> spotifyArtistId.equals(artist.id())))
				.map(this::toTrackSnapshot)
				.filter(track -> track.spotifyTrackId() != null && !track.spotifyTrackId().isBlank())
				.collect(Collectors.toMap(
					SpotifyTrackSnapshot::spotifyTrackId,
					Function.identity(),
					(left, right) -> left,
					java.util.LinkedHashMap::new
				));
			return uniqueTracks.values().stream().limit(limit).toList();
		} catch (RestClientResponseException exception) {
			logSpotifyError(exception);
			return List.of();
		} catch (RestClientException exception) {
			return List.of();
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
	private record ArtistDetail(
		String id,
		String name,
		List<Image> images,
		Integer popularity,
		List<String> genres,
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
	private record TrackSearchResponse(TrackSearchPage tracks) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record TrackSearchPage(List<TrackItem> items) {
		private List<TrackItem> safeItems() {
			return items == null ? List.of() : items;
		}
	}
}
