package com.setpik.server.prestudy.service;

import com.setpik.server.analysis.domain.AnalysisStatus;
import com.setpik.server.analysis.domain.PlaylistAnalysis;
import com.setpik.server.analysis.repository.PlaylistAnalysisRepository;
import com.setpik.server.artist.domain.Artist;
import com.setpik.server.artist.domain.ArtistAlias;
import com.setpik.server.artist.domain.ArtistAliasResolutionStatus;
import com.setpik.server.artist.repository.ArtistAliasRepository;
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
import com.setpik.server.performance.domain.PerformanceMatch;
import com.setpik.server.performance.domain.PerformanceMatchArtist;
import com.setpik.server.performance.repository.PerformanceArtistRepository;
import com.setpik.server.performance.repository.PerformanceMatchArtistRepository;
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
	private final PerformanceMatchArtistRepository performanceMatchArtistRepository;
	private final PlaylistAnalysisRepository playlistAnalysisRepository;
	private final PlaylistTrackRepository playlistTrackRepository;
	private final TrackRepository trackRepository;
	private final ArtistRepository artistRepository;
	private final ArtistAliasRepository artistAliasRepository;
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
		PerformanceMatchArtistRepository performanceMatchArtistRepository,
		PlaylistAnalysisRepository playlistAnalysisRepository,
		PlaylistTrackRepository playlistTrackRepository,
		TrackRepository trackRepository,
		ArtistRepository artistRepository,
		ArtistAliasRepository artistAliasRepository,
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
		this.performanceMatchArtistRepository = performanceMatchArtistRepository;
		this.playlistAnalysisRepository = playlistAnalysisRepository;
		this.playlistTrackRepository = playlistTrackRepository;
		this.trackRepository = trackRepository;
		this.artistRepository = artistRepository;
		this.artistAliasRepository = artistAliasRepository;
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
		PerformanceMatch match = validateMatchedPerformance(analysisId, performanceId);
		List<EffectiveArtist> lineupArtists = resolveEffectiveArtists(performanceId, match);
		Map<Long, List<Track>> originalTracksByArtist =
			findOriginalTracksByArtist(analysis.getPlaylistId());

		String accessToken = null;
		List<PrestudyCandidateResponse.ArtistCandidate> artists = new ArrayList<>();
		for (EffectiveArtist artist : lineupArtists) {
			List<Track> originalTracks = originalTracks(artist, originalTracksByArtist);
			boolean fromOriginal = !originalTracks.isEmpty();
			List<PrestudyCandidateResponse.TrackCandidate> candidateTracks;
			if (fromOriginal) {
                candidateTracks = originalTracks.stream()
                .limit(1)
                .map(track -> new PrestudyCandidateResponse.TrackCandidate(
                    track.getTrackId(), track.getTrackName(), SourceType.ORIGINAL_PLAYLIST.name(),
                    track.getAlbumName(), track.getAlbumImageUrl(), track.getSpotifyTrackUrl(),
                    track.getPreviewUrl(), track.getDurationMs()))
                .toList();
            } else if (artist.spotifyArtistId() == null || artist.spotifyArtistId().isBlank()) {
                candidateTracks = List.of();
            } else {
             if (accessToken == null) {
                accessToken = resolveAccessToken(findConnectedSpotifyAccount(userId));
            }
                candidateTracks = buildRepresentativeTrackCandidate(accessToken, artist);
            }
			artists.add(new PrestudyCandidateResponse.ArtistCandidate(
				artist.displayArtistId(), artist.artistName(), fromOriginal, candidateTracks));
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
		PerformanceMatch match = validateMatchedPerformance(request.analysisId(), performanceId);
		List<SelectedTrack> selectedTracks = resolveSelectedTracks(
			analysis, performanceId, match, request.selectedTrackIds());

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
		PerformanceMatch match,
		List<Long> selectedTrackIds
	) {
		if (selectedTrackIds.isEmpty()) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}

		if (new HashSet<>(selectedTrackIds).size() != selectedTrackIds.size()) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}

		Map<Long, Track> trackById = trackRepository.findAllById(selectedTrackIds).stream()
			.collect(Collectors.toMap(Track::getTrackId, Function.identity()));
		if (trackById.size() != selectedTrackIds.size()) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
		}

		List<EffectiveArtist> effectiveArtists = resolveEffectiveArtists(performanceId, match);
		Set<Long> lineupArtistIds = effectiveArtists.stream()
			.flatMap(artist -> artist.matchableArtistIds().stream())
			.collect(Collectors.toSet());
		Map<Long, List<Track>> originalTracksByArtist =
			findOriginalTracksByArtist(analysis.getPlaylistId());
		Set<Long> originalCandidateTrackIds = effectiveArtists.stream()
			.flatMap(artist -> originalTracks(artist, originalTracksByArtist).stream().limit(1))
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

	private List<Track> originalTracks(
		EffectiveArtist artist,
		Map<Long, List<Track>> originalTracksByArtist
	) {
		LinkedHashMap<Long, Track> tracks = new LinkedHashMap<>();
		for (Long artistId : artist.matchableArtistIds()) {
			for (Track track : originalTracksByArtist.getOrDefault(artistId, List.of())) {
				tracks.putIfAbsent(track.getTrackId(), track);
			}
		}
		return List.copyOf(tracks.values());
	}

	private List<PrestudyCandidateResponse.TrackCandidate> buildRepresentativeTrackCandidate(
		String accessToken,
		EffectiveArtist artist
	) {
		if (artist.spotifyArtistId() == null || artist.spotifyArtistId().isBlank()) {
			return List.of();
		}
		SpotifyTrackSnapshot source = spotifyPlaylistClient.fetchRepresentativeTrack(
			accessToken, artist.spotifyArtistId(), artist.artistName());
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
			track.getTrackId(), artist.trackArtistId())) {
			trackArtistRepository.save(new TrackArtist(
				track.getTrackId(), artist.trackArtistId(), (short) 1));
		}
		return List.of(new PrestudyCandidateResponse.TrackCandidate(
            track.getTrackId(), track.getTrackName(), SourceType.MATCHED_ARTIST.name(),
            track.getAlbumName(), track.getAlbumImageUrl(), track.getSpotifyTrackUrl(),
            track.getPreviewUrl(), track.getDurationMs()));
	}

	private List<EffectiveArtist> resolveEffectiveArtists(
		Long performanceId,
		PerformanceMatch match
	) {
		List<PerformanceArtist> lineup = performanceArtistRepository
			.findByPerformanceIdOrderByLineupOrderAsc(performanceId);
		List<Long> lineupArtistIds = lineup.stream().map(PerformanceArtist::getArtistId).toList();
		Map<Long, Artist> lineupArtistById = (lineupArtistIds.isEmpty()
			? List.<Artist>of() : artistRepository.findAllById(lineupArtistIds)).stream()
			.collect(Collectors.toMap(Artist::getArtistId, Function.identity()));
		Map<Long, ArtistAlias> aliasByKopisArtistId = (lineupArtistIds.isEmpty()
			? List.<ArtistAlias>of() : artistAliasRepository.findByKopisArtistIdIn(lineupArtistIds)).stream()
			.filter(alias -> alias.getResolutionStatus() == ArtistAliasResolutionStatus.RESOLVED)
			.filter(alias -> alias.getSpotifyArtistId() != null)
			.collect(Collectors.toMap(ArtistAlias::getKopisArtistId, Function.identity(),
				(left, right) -> left));
		List<String> aliasSpotifyIds = aliasByKopisArtistId.values().stream()
			.map(ArtistAlias::getSpotifyArtistId).distinct().toList();
		Map<String, Artist> spotifyArtistBySpotifyId = (aliasSpotifyIds.isEmpty()
			? List.<Artist>of() : artistRepository.findBySpotifyArtistIdIn(aliasSpotifyIds)).stream()
			.collect(Collectors.toMap(Artist::getSpotifyArtistId, Function.identity(),
				(left, right) -> left));

		LinkedHashMap<String, EffectiveArtist> resolved = new LinkedHashMap<>();
		for (PerformanceArtist mapping : lineup) {
			Artist lineupArtist = lineupArtistById.get(mapping.getArtistId());
			if (lineupArtist == null) continue;
			ArtistAlias alias = aliasByKopisArtistId.get(lineupArtist.getArtistId());
			String spotifyId = lineupArtist.getSpotifyArtistId();
			Artist spotifyArtist = spotifyId == null || spotifyId.isBlank() ? null : lineupArtist;
			if ((spotifyId == null || spotifyId.isBlank()) && alias != null) {
				spotifyId = alias.getSpotifyArtistId();
				spotifyArtist = spotifyArtistBySpotifyId.get(spotifyId);
			}
			Long spotifyDatabaseArtistId = spotifyArtist == null ? null : spotifyArtist.getArtistId();
			Set<Long> matchableIds = new HashSet<>();
			matchableIds.add(lineupArtist.getArtistId());
			if (spotifyDatabaseArtistId != null) matchableIds.add(spotifyDatabaseArtistId);
			EffectiveArtist effective = new EffectiveArtist(
				lineupArtist.getArtistId(), lineupArtist.getArtistName(), spotifyId,
				spotifyDatabaseArtistId == null ? lineupArtist.getArtistId() : spotifyDatabaseArtistId,
				Set.copyOf(matchableIds));
			resolved.putIfAbsent(effective.identityKey(), effective);
		}

		if (match.getMatchId() != null) {
			List<PerformanceMatchArtist> matchArtists = performanceMatchArtistRepository
				.findByMatchId(match.getMatchId());
			List<Long> matchedArtistIds = matchArtists.stream()
				.map(PerformanceMatchArtist::getArtistId).toList();
			Map<Long, Artist> matchedArtistById = (matchedArtistIds.isEmpty()
				? List.<Artist>of() : artistRepository.findAllById(matchedArtistIds)).stream()
				.collect(Collectors.toMap(Artist::getArtistId, Function.identity()));
			for (PerformanceMatchArtist matchArtist : matchArtists) {
				Artist artist = matchedArtistById.get(matchArtist.getArtistId());
				if (artist == null) continue;
				EffectiveArtist effective = new EffectiveArtist(
					artist.getArtistId(), artist.getArtistName(), artist.getSpotifyArtistId(),
					artist.getArtistId(), Set.of(artist.getArtistId()));
				resolved.putIfAbsent(effective.identityKey(), effective);
			}
		}
		return List.copyOf(resolved.values());
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

	private PerformanceMatch validateMatchedPerformance(Long analysisId, Long performanceId) {
		return performanceMatchRepository.findByAnalysisIdAndPerformanceId(analysisId, performanceId)
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

	private record EffectiveArtist(
		Long displayArtistId,
		String artistName,
		String spotifyArtistId,
		Long trackArtistId,
		Set<Long> matchableArtistIds
	) {
		private String identityKey() {
			return spotifyArtistId == null || spotifyArtistId.isBlank()
				? "artist:" + displayArtistId : "spotify:" + spotifyArtistId;
		}
	}
}
