package com.setpik.server.prestudy.service;

import com.setpik.server.analysis.domain.AnalysisStatus;
import com.setpik.server.analysis.domain.PlaylistAnalysis;
import com.setpik.server.analysis.repository.PlaylistAnalysisRepository;
import com.setpik.server.artist.domain.Artist;
import com.setpik.server.artist.repository.ArtistRepository;
import com.setpik.server.auth.client.SpotifyApiException;
import com.setpik.server.auth.client.SpotifyOAuthClient;
import com.setpik.server.auth.client.dto.SpotifyTokenResponse;
import com.setpik.server.auth.security.TokenCipher;
import com.setpik.server.common.api.PageResponse;
import com.setpik.server.common.exception.BusinessException;
import com.setpik.server.common.exception.ErrorCode;
import com.setpik.server.performance.domain.Performance;
import com.setpik.server.performance.domain.PerformanceArtist;
import com.setpik.server.performance.repository.PerformanceArtistRepository;
import com.setpik.server.performance.repository.PerformanceMatchRepository;
import com.setpik.server.performance.repository.PerformanceRepository;
import com.setpik.server.playlist.client.SpotifyPlaylistApiException;
import com.setpik.server.playlist.client.SpotifyPlaylistClient;
import com.setpik.server.playlist.client.dto.SpotifyTrackSnapshot;
import com.setpik.server.playlist.domain.PlaylistTrack;
import com.setpik.server.playlist.domain.Track;
import com.setpik.server.playlist.domain.TrackArtist;
import com.setpik.server.playlist.repository.PlaylistTrackRepository;
import com.setpik.server.playlist.repository.TrackArtistRepository;
import com.setpik.server.playlist.repository.TrackRepository;
import com.setpik.server.prestudy.domain.PrestudyPlaylist;
import com.setpik.server.prestudy.domain.PrestudyPlaylistTrack;
import com.setpik.server.prestudy.domain.SourceType;
import com.setpik.server.prestudy.dto.CreatePrestudyPlaylistRequest;
import com.setpik.server.prestudy.dto.CreatePrestudyPlaylistResponse;
import com.setpik.server.prestudy.dto.PrestudyCandidateResponse;
import com.setpik.server.prestudy.dto.PrestudyPlaylistDetailResponse;
import com.setpik.server.prestudy.dto.PrestudyPlaylistSummaryResponse;
import com.setpik.server.prestudy.dto.PrestudyPlaylistTrackResponse;
import com.setpik.server.prestudy.repository.PrestudyPlaylistRepository;
import com.setpik.server.prestudy.repository.PrestudyPlaylistTrackRepository;
import com.setpik.server.spotify.domain.ConnectionStatus;
import com.setpik.server.spotify.domain.SpotifyAccount;
import com.setpik.server.spotify.repository.SpotifyAccountRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PrestudyPlaylistService {

	private final PrestudyPlaylistRepository prestudyPlaylistRepository;
	private final PrestudyPlaylistTrackRepository prestudyPlaylistTrackRepository;
	private final PerformanceRepository performanceRepository;
	private final PerformanceArtistRepository performanceArtistRepository;
	private final PerformanceMatchRepository performanceMatchRepository;
	private final PlaylistAnalysisRepository playlistAnalysisRepository;
	private final PlaylistTrackRepository playlistTrackRepository;
	private final TrackRepository trackRepository;
	private final ArtistRepository artistRepository;
	private final TrackArtistRepository trackArtistRepository;
	private final SpotifyAccountRepository spotifyAccountRepository;
	private final SpotifyPlaylistClient spotifyPlaylistClient;
	private final SpotifyOAuthClient spotifyOAuthClient;
	private final TokenCipher tokenCipher;
	private final Clock clock;

	public PrestudyPlaylistService(
		PrestudyPlaylistRepository prestudyPlaylistRepository,
		PrestudyPlaylistTrackRepository prestudyPlaylistTrackRepository,
		PerformanceRepository performanceRepository,
		PerformanceArtistRepository performanceArtistRepository,
		PerformanceMatchRepository performanceMatchRepository,
		PlaylistAnalysisRepository playlistAnalysisRepository,
		PlaylistTrackRepository playlistTrackRepository,
		TrackRepository trackRepository,
		ArtistRepository artistRepository,
		TrackArtistRepository trackArtistRepository,
		SpotifyAccountRepository spotifyAccountRepository,
		SpotifyPlaylistClient spotifyPlaylistClient,
		SpotifyOAuthClient spotifyOAuthClient,
		TokenCipher tokenCipher,
		Clock clock
	) {
		this.prestudyPlaylistRepository = prestudyPlaylistRepository;
		this.prestudyPlaylistTrackRepository = prestudyPlaylistTrackRepository;
		this.performanceRepository = performanceRepository;
		this.performanceArtistRepository = performanceArtistRepository;
		this.performanceMatchRepository = performanceMatchRepository;
		this.playlistAnalysisRepository = playlistAnalysisRepository;
		this.playlistTrackRepository = playlistTrackRepository;
		this.trackRepository = trackRepository;
		this.artistRepository = artistRepository;
		this.trackArtistRepository = trackArtistRepository;
		this.spotifyAccountRepository = spotifyAccountRepository;
		this.spotifyPlaylistClient = spotifyPlaylistClient;
		this.spotifyOAuthClient = spotifyOAuthClient;
		this.tokenCipher = tokenCipher;
		this.clock = clock;
	}

	public PageResponse<PrestudyPlaylistSummaryResponse> getMyPrestudyPlaylists(
		Long userId,
		Pageable pageable
	) {
		Page<PrestudyPlaylist> playlists = prestudyPlaylistRepository.findByUserId(userId, pageable);
		Map<Long, Performance> performanceById = performanceRepository
			.findAllById(playlists.getContent().stream()
				.map(PrestudyPlaylist::getPerformanceId).distinct().toList())
			.stream()
			.collect(Collectors.toMap(Performance::getPerformanceId, Function.identity()));

		List<PrestudyPlaylistSummaryResponse> content = playlists.getContent().stream()
			.map(playlist -> {
				Performance performance = performanceById.get(playlist.getPerformanceId());
				return PrestudyPlaylistSummaryResponse.of(
					playlist,
					performance == null ? null : performance.getPerformanceName(),
					performance == null ? null : performance.getPosterUrl()
				);
			})
			.toList();
		return PageResponse.of(content, playlists);
	}

	public PrestudyPlaylistDetailResponse getPrestudyPlaylist(Long userId, Long prestudyPlaylistId) {
		return PrestudyPlaylistDetailResponse.from(findOwned(userId, prestudyPlaylistId));
	}

	public List<PrestudyPlaylistTrackResponse> getPrestudyPlaylistTracks(
		Long userId,
		Long prestudyPlaylistId
	) {
		findOwned(userId, prestudyPlaylistId);
		List<PrestudyPlaylistTrack> playlistTracks = prestudyPlaylistTrackRepository
			.findByPrestudyPlaylistIdOrderByTrackOrderAsc(prestudyPlaylistId);
		Map<Long, Track> trackById = trackRepository
			.findAllById(playlistTracks.stream().map(PrestudyPlaylistTrack::getTrackId).toList())
			.stream()
			.collect(Collectors.toMap(Track::getTrackId, Function.identity()));

		return playlistTracks.stream()
			.map(playlistTrack -> {
				Track track = trackById.get(playlistTrack.getTrackId());
				if (track == null) {
					throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
				}
				return PrestudyPlaylistTrackResponse.of(playlistTrack, track);
			})
			.toList();
	}

	/** Spotify 대표곡을 내부 Track ID로 반환해야 하므로 조회 중 캐시 저장을 허용한다. */
	@Transactional
	public PrestudyCandidateResponse getCandidates(Long userId, Long performanceId, Long analysisId) {
		PlaylistAnalysis analysis = findCompletedOwnedAnalysis(userId, analysisId);
		findActivePerformance(performanceId);
		validateMatchedPerformance(analysisId, performanceId);

		List<PerformanceArtist> lineupArtists =
			performanceArtistRepository.findByPerformanceIdOrderByLineupOrderAsc(performanceId);
		Map<Long, Artist> artistById = artistRepository
			.findAllById(lineupArtists.stream().map(PerformanceArtist::getArtistId).toList())
			.stream()
			.collect(Collectors.toMap(Artist::getArtistId, Function.identity()));
		Map<Long, List<Track>> originalTracksByArtist =
			findOriginalTracksByArtist(analysis.getPlaylistId());

		String accessToken = null;
		List<PrestudyCandidateResponse.ArtistCandidate> artists = new ArrayList<>();
		for (PerformanceArtist lineupArtist : lineupArtists) {
			Artist artist = artistById.get(lineupArtist.getArtistId());
			if (artist == null) {
				throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
			}
			boolean fromOriginal = originalTracksByArtist.containsKey(artist.getArtistId());
			List<PrestudyCandidateResponse.TrackCandidate> candidateTracks;
			if (fromOriginal) {
				candidateTracks = originalTracksByArtist.get(artist.getArtistId()).stream()
					.map(track -> new PrestudyCandidateResponse.TrackCandidate(
						track.getTrackId(), track.getTrackName(), SourceType.ORIGINAL_PLAYLIST.name()))
					.toList();
			} else {
				if (accessToken == null) {
					accessToken = resolveAccessToken(findConnectedSpotifyAccount(userId));
				}
				candidateTracks = buildRepresentativeTrackCandidate(accessToken, artist);
			}
			artists.add(new PrestudyCandidateResponse.ArtistCandidate(
				artist.getArtistId(), artist.getArtistName(), fromOriginal, candidateTracks));
		}

		return new PrestudyCandidateResponse(performanceId, analysisId, artists);
	}

	@Transactional
	public CreatePrestudyPlaylistResponse createPrestudyPlaylist(
		Long userId,
		Long performanceId,
		CreatePrestudyPlaylistRequest request
	) {
		PlaylistAnalysis analysis = findCompletedOwnedAnalysis(userId, request.analysisId());
		findActivePerformance(performanceId);
		validateMatchedPerformance(request.analysisId(), performanceId);
		List<SelectedTrack> selectedTracks = resolveSelectedTracks(
			analysis, performanceId, request.selectedTrackIds());

		SpotifyAccount account = findConnectedSpotifyAccount(userId);
		String accessToken = resolveAccessToken(account);
		String spotifyPlaylistId;
		try {
			spotifyPlaylistId = spotifyPlaylistClient.createPlaylist(
				accessToken, request.playlistTitle(), request.isPublic());
			spotifyPlaylistClient.addTracks(
				accessToken,
				spotifyPlaylistId,
				selectedTracks.stream().map(item -> item.track().getSpotifyTrackId()).toList()
			);
		} catch (SpotifyPlaylistApiException exception) {
			if (exception.requiresReauthentication()) {
				throw new BusinessException(ErrorCode.SPOTIFY_REAUTHENTICATION_REQUIRED);
			}
			throw new BusinessException(ErrorCode.PRESTUDY_PLAYLIST_CREATION_FAILED);
		}

		PrestudyPlaylist playlist = prestudyPlaylistRepository.save(new PrestudyPlaylist(
			request.playlistTitle(), request.isPublic(), userId,
			performanceId, request.analysisId()));
		int order = 1;
		for (SelectedTrack selectedTrack : selectedTracks) {
			prestudyPlaylistTrackRepository.save(new PrestudyPlaylistTrack(
				playlist.getPrestudyPlaylistId(), selectedTrack.track().getTrackId(), order++,
				selectedTrack.sourceType(), selectedTrack.isNewArtistTrack()));
		}
		playlist.markCompleted(spotifyPlaylistId, selectedTracks.size());

		return new CreatePrestudyPlaylistResponse(
			playlist.getPrestudyPlaylistId(), spotifyPlaylistId, selectedTracks.size());
	}

	private List<SelectedTrack> resolveSelectedTracks(
		PlaylistAnalysis analysis,
		Long performanceId,
		List<Long> selectedTrackIds
	) {
		if (new HashSet<>(selectedTrackIds).size() != selectedTrackIds.size()) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}

		Map<Long, Track> trackById = trackRepository.findAllById(selectedTrackIds).stream()
			.collect(Collectors.toMap(Track::getTrackId, Function.identity()));
		if (trackById.size() != selectedTrackIds.size()) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
		}

		Set<Long> lineupArtistIds = performanceArtistRepository
			.findByPerformanceIdOrderByLineupOrderAsc(performanceId).stream()
			.map(PerformanceArtist::getArtistId)
			.collect(Collectors.toSet());
		Map<Long, List<Track>> originalTracksByArtist =
			findOriginalTracksByArtist(analysis.getPlaylistId());
		Set<Long> originalCandidateTrackIds = originalTracksByArtist.entrySet().stream()
			.filter(entry -> lineupArtistIds.contains(entry.getKey()))
			.flatMap(entry -> entry.getValue().stream())
			.map(Track::getTrackId)
			.collect(Collectors.toSet());
		Map<Long, Set<Long>> artistIdsByTrack = trackArtistRepository
			.findByTrackIdInOrderByTrackIdAscArtistOrderAsc(selectedTrackIds).stream()
			.collect(Collectors.groupingBy(
				TrackArtist::getTrackId,
				Collectors.mapping(TrackArtist::getArtistId, Collectors.toSet())
			));

		List<SelectedTrack> result = new ArrayList<>();
		for (Long trackId : selectedTrackIds) {
			boolean original = originalCandidateTrackIds.contains(trackId);
			boolean matchedArtistTrack = !original
				&& artistIdsByTrack.getOrDefault(trackId, Set.of()).stream()
					.anyMatch(lineupArtistIds::contains);
			if (!original && !matchedArtistTrack) {
				throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
			}
			result.add(new SelectedTrack(
				trackById.get(trackId),
				original ? SourceType.ORIGINAL_PLAYLIST : SourceType.MATCHED_ARTIST,
				!original
			));
		}
		return result;
	}

	private List<PrestudyCandidateResponse.TrackCandidate> buildRepresentativeTrackCandidate(
		String accessToken,
		Artist artist
	) {
		if (artist.getSpotifyArtistId() == null || artist.getSpotifyArtistId().isBlank()) {
			return List.of();
		}
		SpotifyTrackSnapshot source = spotifyPlaylistClient.fetchRepresentativeTrack(
			accessToken, artist.getSpotifyArtistId(), artist.getArtistName());
		if (source == null) {
			return List.of();
		}

		Track track = trackRepository.findBySpotifyTrackId(source.spotifyTrackId())
			.orElseGet(() -> trackRepository.save(new Track(
				source.spotifyTrackId(), source.trackName(), source.albumName(),
				source.albumImageUrl(), source.spotifyTrackUrl(), source.previewUrl(),
				source.durationMs(), source.isPlayable()
			)));
		if (!trackArtistRepository.existsByTrackIdAndArtistId(
			track.getTrackId(), artist.getArtistId())) {
			trackArtistRepository.save(new TrackArtist(
				track.getTrackId(), artist.getArtistId(), (short) 1));
		}
		return List.of(new PrestudyCandidateResponse.TrackCandidate(
			track.getTrackId(), track.getTrackName(), SourceType.MATCHED_ARTIST.name()));
	}

	private Map<Long, List<Track>> findOriginalTracksByArtist(Long playlistId) {
		List<PlaylistTrack> playlistTracks =
			playlistTrackRepository.findByPlaylistIdOrderByTrackPositionAsc(playlistId);
		List<Long> trackIds = playlistTracks.stream().map(PlaylistTrack::getTrackId).distinct().toList();
		if (trackIds.isEmpty()) {
			return Map.of();
		}

		Map<Long, Track> trackById = trackRepository.findAllById(trackIds).stream()
			.collect(Collectors.toMap(Track::getTrackId, Function.identity()));
		Map<Long, List<Long>> artistIdsByTrack = trackArtistRepository
			.findByTrackIdInOrderByTrackIdAscArtistOrderAsc(trackIds).stream()
			.collect(Collectors.groupingBy(
				TrackArtist::getTrackId,
				Collectors.mapping(TrackArtist::getArtistId, Collectors.toList())
			));
		Map<Long, LinkedHashMap<Long, Track>> tracksByArtist = new HashMap<>();
		for (PlaylistTrack playlistTrack : playlistTracks) {
			Track track = trackById.get(playlistTrack.getTrackId());
			if (track == null) {
				continue;
			}
			for (Long artistId : artistIdsByTrack.getOrDefault(track.getTrackId(), List.of())) {
				tracksByArtist.computeIfAbsent(artistId, ignored -> new LinkedHashMap<>())
					.putIfAbsent(track.getTrackId(), track);
			}
		}
		return tracksByArtist.entrySet().stream()
			.collect(Collectors.toMap(Map.Entry::getKey, entry -> List.copyOf(entry.getValue().values())));
	}

	private PlaylistAnalysis findCompletedOwnedAnalysis(Long userId, Long analysisId) {
		PlaylistAnalysis analysis = playlistAnalysisRepository
			.findByAnalysisIdAndUserId(analysisId, userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
		if (analysis.getAnalysisStatus() != AnalysisStatus.COMPLETED) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
		}
		return analysis;
	}

	private Performance findActivePerformance(Long performanceId) {
		return performanceRepository.findByPerformanceIdAndIsDeletedFalse(performanceId)
			.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
	}

	private void validateMatchedPerformance(Long analysisId, Long performanceId) {
		performanceMatchRepository.findByAnalysisIdAndPerformanceId(analysisId, performanceId)
			.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
	}

	private SpotifyAccount findConnectedSpotifyAccount(Long userId) {
		return spotifyAccountRepository.findByUserId(userId)
			.filter(account -> account.getConnectionStatus() == ConnectionStatus.CONNECTED)
			.orElseThrow(() -> new BusinessException(ErrorCode.SPOTIFY_CONNECTION_REQUIRED));
	}

	private String resolveAccessToken(SpotifyAccount account) {
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

	private PrestudyPlaylist findOwned(Long userId, Long prestudyPlaylistId) {
		return prestudyPlaylistRepository.findByPrestudyPlaylistIdAndUserId(prestudyPlaylistId, userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
	}

	private record SelectedTrack(
		Track track,
		SourceType sourceType,
		boolean isNewArtistTrack
	) {
	}
}
