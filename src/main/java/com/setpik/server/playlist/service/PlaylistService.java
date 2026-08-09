package com.setpik.server.playlist.service;

import com.setpik.server.auth.client.SpotifyApiException;
import com.setpik.server.auth.client.SpotifyOAuthClient;
import com.setpik.server.auth.client.dto.SpotifyTokenResponse;
import com.setpik.server.auth.security.TokenCipher;
import com.setpik.server.artist.domain.Artist;
import com.setpik.server.artist.repository.ArtistRepository;
import com.setpik.server.common.exception.BusinessException;
import com.setpik.server.common.exception.ErrorCode;
import com.setpik.server.playlist.client.SpotifyPlaylistApiException;
import com.setpik.server.playlist.client.SpotifyPlaylistClient;
import com.setpik.server.playlist.client.dto.SpotifyArtistSnapshot;
import com.setpik.server.playlist.client.dto.SpotifyPlaylistSnapshot;
import com.setpik.server.playlist.client.dto.SpotifyTrackSnapshot;
import com.setpik.server.playlist.domain.PlaylistTrack;
import com.setpik.server.playlist.domain.SpotifyPlaylist;
import com.setpik.server.playlist.domain.Track;
import com.setpik.server.playlist.domain.TrackArtist;
import com.setpik.server.playlist.dto.PlaylistDetailResponse;
import com.setpik.server.playlist.dto.PlaylistPageResponse;
import com.setpik.server.playlist.dto.PlaylistSummaryResponse;
import com.setpik.server.playlist.dto.PlaylistSyncResponse;
import com.setpik.server.playlist.dto.TrackResponse;
import com.setpik.server.playlist.dto.TrackArtistResponse;
import com.setpik.server.playlist.dto.TrackPageResponse;
import com.setpik.server.playlist.repository.*;
import com.setpik.server.spotify.domain.ConnectionStatus;
import com.setpik.server.spotify.domain.SpotifyAccount;
import com.setpik.server.spotify.repository.SpotifyAccountRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.Set;
import com.setpik.server.common.api.PageResponse;
import com.setpik.server.playlist.domain.PlaylistRecentSelection;
import com.setpik.server.playlist.domain.PlaylistRecentSelectionId;
import com.setpik.server.playlist.dto.PlaylistSelectResponse;
import com.setpik.server.playlist.dto.RecentSelectionResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PlaylistService {
	private static final int MAX_PAGE_SIZE = 100;
	private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
		"playlistId", "playlistName", "trackCount", "lastSyncedAt"
	);
	private static final Set<String> ALLOWED_TRACK_SORT_FIELDS = Set.of(
		"playlistTrackId", "trackPosition"
	);

	private final SpotifyPlaylistRepository playlistRepository;
	private final PlaylistTrackRepository playlistTrackRepository;
	private final TrackRepository trackRepository;
	private final ArtistRepository artistRepository;
	private final TrackArtistRepository trackArtistRepository;
	private final SpotifyAccountRepository spotifyAccountRepository;
	private final PlaylistRecentSelectionRepository recentSelectionRepository;
	private final SpotifyPlaylistClient spotifyClient;
	private final SpotifyOAuthClient spotifyOAuthClient;
	private final TokenCipher tokenCipher;
	private final Clock clock;

	public PlaylistService(SpotifyPlaylistRepository playlistRepository,
						   PlaylistTrackRepository playlistTrackRepository,
						   TrackRepository trackRepository,
						   ArtistRepository artistRepository,
						   TrackArtistRepository trackArtistRepository,
						   SpotifyAccountRepository spotifyAccountRepository,
						   PlaylistRecentSelectionRepository recentSelectionRepository,
						   SpotifyPlaylistClient spotifyClient,
						   SpotifyOAuthClient spotifyOAuthClient,
						   TokenCipher tokenCipher,
						   Clock clock) {
		this.playlistRepository = playlistRepository;
		this.playlistTrackRepository = playlistTrackRepository;
		this.trackRepository = trackRepository;
		this.artistRepository = artistRepository;
		this.trackArtistRepository = trackArtistRepository;
		this.spotifyAccountRepository = spotifyAccountRepository;
		this.recentSelectionRepository = recentSelectionRepository;
		this.spotifyClient = spotifyClient;
		this.spotifyOAuthClient = spotifyOAuthClient;
		this.tokenCipher = tokenCipher;
		this.clock = clock;
	}

	@Transactional
	public PlaylistSyncResponse sync(Long userId) {
		String accessToken = resolveAccessToken(userId);
		List<SpotifyPlaylistSnapshot> fetched;
		try {
			fetched = spotifyClient.fetchMyPlaylists(accessToken);
		} catch (SpotifyPlaylistApiException exception) {
			throw new BusinessException(ErrorCode.SPOTIFY_API_ERROR);
		}

		int syncedTrackCount = 0;

		for (SpotifyPlaylistSnapshot source : fetched) {
			SpotifyPlaylist playlist = playlistRepository
				.findByUserIdAndSpotifyPlaylistId(userId, source.spotifyPlaylistId())
				.orElse(null);

			if (playlist == null) {
				playlist = new SpotifyPlaylist(
					source.spotifyPlaylistId(),
					source.playlistName(),
					source.description(),
					source.coverImageUrl(),
					source.isPublic(),
					source.ownerSpotifyUserId(),
					source.snapshotId(),
					source.tracks().size(),
					userId
				);
				playlist = playlistRepository.save(playlist);
			} else {
				playlist.syncFrom(
					source.playlistName(),
					source.description(),
					source.coverImageUrl(),
					source.isPublic(),
					source.ownerSpotifyUserId(),
					source.snapshotId(),
					source.tracks().size()
				);
			}

			syncedTrackCount += syncTracks(playlist.getPlaylistId(), source.tracks());
		}

		return new PlaylistSyncResponse(
			fetched.size(),
			syncedTrackCount,
			OffsetDateTime.now(clock)
		);
	}

	private int syncTracks(Long playlistId, List<SpotifyTrackSnapshot> sourceTracks) {
		playlistTrackRepository.deleteByPlaylistId(playlistId);

		int position = 1;
		for (SpotifyTrackSnapshot source : sourceTracks) {
			Track track = trackRepository
				.findBySpotifyTrackId(source.spotifyTrackId())
				.orElse(null);
			if (track == null) {
				track = trackRepository.save(new Track(
					source.spotifyTrackId(), source.trackName(), source.albumName(),
					source.albumImageUrl(), source.spotifyTrackUrl(), source.previewUrl(),
					source.durationMs(), source.isPlayable()
				));
			} else {
				track.syncFrom(
					source.trackName(), source.albumName(), source.albumImageUrl(),
					source.spotifyTrackUrl(), source.previewUrl(), source.durationMs(),
					source.isPlayable()
				);
			}
			syncArtists(track.getTrackId(), source.artists());

			playlistTrackRepository.save(new PlaylistTrack(
				playlistId,
				track.getTrackId(),
				position++,
				source.addedAt() == null ? LocalDateTime.now(clock) : source.addedAt()
			));
		}
		return sourceTracks.size();
	}

	private void syncArtists(Long trackId, List<SpotifyArtistSnapshot> sourceArtists) {
		trackArtistRepository.deleteByTrackId(trackId);
		trackArtistRepository.flush();
		if (sourceArtists == null) {
			return;
		}

		short artistOrder = 1;
		for (SpotifyArtistSnapshot source : sourceArtists) {
			Artist artist = artistRepository.findBySpotifyArtistId(source.spotifyArtistId())
				.orElse(null);
			if (artist == null) {
				artist = artistRepository.save(new Artist(
					source.spotifyArtistId(), source.artistName(), source.spotifyArtistUrl()
				));
			} else {
				artist.syncFromSpotify(source.artistName(), source.spotifyArtistUrl());
			}
			trackArtistRepository.save(new TrackArtist(trackId, artist.getArtistId(), artistOrder++));
		}
	}

	private String resolveAccessToken(Long userId) {
		SpotifyAccount account = spotifyAccountRepository.findByUserId(userId)
			.filter(candidate -> candidate.getConnectionStatus() == ConnectionStatus.CONNECTED)
			.orElseThrow(() -> new BusinessException(ErrorCode.SPOTIFY_CONNECTION_REQUIRED));
		LocalDateTime now = LocalDateTime.now(clock);

		if (account.getTokenExpiresAt() != null
			&& account.getTokenExpiresAt().isAfter(now.plusSeconds(30))) {
			try {
				return decryptRequired(account.getAccessTokenEncrypted());
			} catch (IllegalStateException exception) {
				throw new BusinessException(ErrorCode.SPOTIFY_REAUTHENTICATION_REQUIRED);
			}
		}

		try {
			String refreshToken = decryptRequired(account.getRefreshTokenEncrypted());
			SpotifyTokenResponse refreshed = spotifyOAuthClient.refreshAccessToken(refreshToken);
			String encryptedRefreshToken = refreshed.refreshToken() == null
				? null
				: tokenCipher.encrypt(refreshed.refreshToken());
			account.refreshTokens(
				tokenCipher.encrypt(refreshed.accessToken()),
				encryptedRefreshToken,
				now.plusSeconds(refreshed.expiresIn())
			);
			return refreshed.accessToken();
		} catch (SpotifyApiException | IllegalStateException exception) {
			throw new BusinessException(ErrorCode.SPOTIFY_REAUTHENTICATION_REQUIRED);
		}
	}

	private String decryptRequired(String encryptedToken) {
		String token = tokenCipher.decrypt(encryptedToken);
		if (token == null || token.isBlank()) {
			throw new IllegalStateException("저장된 Spotify 토큰이 없습니다.");
		}
		return token;
	}

	public PlaylistPageResponse getMyPlaylists(
		Long userId,
		int page,
		int size,
		String sort,
		String keyword
	) {
		if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}

		Sort sorting = parsePlaylistSort(sort);
		PageRequest pageable = PageRequest.of(page, size, sorting);
		Page<PlaylistSummaryResponse> result = playlistRepository
			.findByUserIdAndDeletedAtIsNullAndPlaylistNameContainingIgnoreCase(
				userId,
				keyword == null ? "" : keyword.trim(),
				pageable
			)
			.map(PlaylistSummaryResponse::from);
		return PlaylistPageResponse.from(result);
	}

	private Sort parsePlaylistSort(String sort) {
		String[] parts = sort == null ? new String[0] : sort.trim().split(",");
		if (parts.length != 2 || !ALLOWED_SORT_FIELDS.contains(parts[0])) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}

		Sort.Direction direction;
		try {
			direction = Sort.Direction.fromString(parts[1]);
		} catch (IllegalArgumentException exception) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}
		return Sort.by(direction, parts[0]);
	}

	public PlaylistDetailResponse getPlaylistDetail(Long userId, Long playlistId) {
		SpotifyPlaylist playlist = findOwnedPlaylist(userId, playlistId);
		return PlaylistDetailResponse.from(playlist);
	}

	public TrackPageResponse getPlaylistTracks(
		Long userId,
		Long playlistId,
		int page,
		int size,
		String sort
	) {
		findOwnedPlaylist(userId, playlistId);
		if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}

		Page<PlaylistTrack> playlistTracks = playlistTrackRepository.findByPlaylistId(
			playlistId,
			PageRequest.of(page, size, parseTrackSort(sort))
		);

		List<Long> trackIds = playlistTracks.getContent().stream()
			.map(PlaylistTrack::getTrackId)
			.distinct()
			.toList();

		Map<Long, Track> trackMap = trackRepository.findAllById(trackIds).stream()
			.collect(Collectors.toMap(Track::getTrackId, Function.identity()));
		List<TrackArtist> trackArtists = trackArtistRepository
			.findByTrackIdInOrderByTrackIdAscArtistOrderAsc(trackIds);
		List<Long> artistIds = trackArtists.stream()
			.map(TrackArtist::getArtistId)
			.distinct()
			.toList();
		Map<Long, Artist> artistMap = artistRepository.findAllById(artistIds).stream()
			.collect(Collectors.toMap(Artist::getArtistId, Function.identity()));
		Map<Long, List<TrackArtistResponse>> artistsByTrackId = new HashMap<>();
		for (TrackArtist trackArtist : trackArtists) {
			Artist artist = artistMap.get(trackArtist.getArtistId());
			if (artist != null) {
				artistsByTrackId.computeIfAbsent(trackArtist.getTrackId(), ignored -> new ArrayList<>())
					.add(TrackArtistResponse.from(artist));
			}
		}

		Page<TrackResponse> result = playlistTracks
			.map(pt -> {
				Track track = trackMap.get(pt.getTrackId());
				if (track == null) {
					throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
				}
				return TrackResponse.of(
					pt,
					track,
					artistsByTrackId.getOrDefault(pt.getTrackId(), List.of())
				);
			});
		return TrackPageResponse.from(result);
	}

	private Sort parseTrackSort(String sort) {
		String[] parts = sort == null ? new String[0] : sort.trim().split(",");
		if (parts.length != 2 || !ALLOWED_TRACK_SORT_FIELDS.contains(parts[0])) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}
		try {
			return Sort.by(Sort.Direction.fromString(parts[1]), parts[0]);
		} catch (IllegalArgumentException exception) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}
	}

	private SpotifyPlaylist findOwnedPlaylist(Long userId, Long playlistId) {
		if (playlistId == null || playlistId < 1) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}
		return playlistRepository
			.findByPlaylistIdAndUserIdAndDeletedAtIsNull(playlistId, userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
	}
	@Transactional
	public PlaylistSelectResponse select(Long userId, Long playlistId) {
		findOwnedPlaylist(userId, playlistId);

		PlaylistRecentSelection selection = recentSelectionRepository
			.findById(new PlaylistRecentSelectionId(userId, playlistId))
			.orElse(null);

		if (selection == null) {
			selection = recentSelectionRepository.save(
				new PlaylistRecentSelection(userId, playlistId));
		} else {
			selection.reselect();
		}

		return PlaylistSelectResponse.from(selection);
	}

	public PageResponse<RecentSelectionResponse> getRecentSelections(Long userId, Pageable pageable) {
		Page<PlaylistRecentSelection> page = recentSelectionRepository.findByUserId(userId, pageable);

		List<Long> playlistIds = page.getContent().stream()
			.map(PlaylistRecentSelection::getPlaylistId)
			.toList();

		Map<Long, String> namesById = playlistRepository.findAllById(playlistIds).stream()
			.collect(Collectors.toMap(
				SpotifyPlaylist::getPlaylistId, SpotifyPlaylist::getPlaylistName));

		List<RecentSelectionResponse> content = page.getContent().stream()
			.map(selection -> new RecentSelectionResponse(
				selection.getPlaylistId(),
				namesById.getOrDefault(selection.getPlaylistId(), "Unknown"),
				selection.getSelectedAt()
			))
			.toList();

		return PageResponse.of(content, page);
	}
}
