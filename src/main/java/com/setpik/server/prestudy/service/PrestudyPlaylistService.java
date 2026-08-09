package com.setpik.server.prestudy.service;

import com.setpik.server.analysis.domain.AnalysisArtist;
import com.setpik.server.analysis.repository.AnalysisArtistRepository;
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
import com.setpik.server.performance.repository.PerformanceRepository;
import com.setpik.server.playlist.client.SpotifyPlaylistApiException;
import com.setpik.server.playlist.client.SpotifyPlaylistClient;
import com.setpik.server.playlist.client.dto.SpotifyTrackSnapshot;
import com.setpik.server.playlist.domain.Track;
import com.setpik.server.playlist.domain.TrackArtist;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PrestudyPlaylistService {

	private final PrestudyPlaylistRepository prestudyPlaylistRepository;
	private final PrestudyPlaylistTrackRepository prestudyPlaylistTrackRepository;
	private final PerformanceRepository performanceRepository;
	private final PerformanceArtistRepository performanceArtistRepository;
	private final TrackRepository trackRepository;
	private final ArtistRepository artistRepository;
	private final AnalysisArtistRepository analysisArtistRepository;
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
		TrackRepository trackRepository,
		ArtistRepository artistRepository,
		AnalysisArtistRepository analysisArtistRepository,
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
		this.trackRepository = trackRepository;
		this.artistRepository = artistRepository;
		this.analysisArtistRepository = analysisArtistRepository;
		this.trackArtistRepository = trackArtistRepository;
		this.spotifyAccountRepository = spotifyAccountRepository;
		this.spotifyPlaylistClient = spotifyPlaylistClient;
		this.spotifyOAuthClient = spotifyOAuthClient;
		this.tokenCipher = tokenCipher;
		this.clock = clock;
	}

	// 예습 플레이리스트 목록 조회
	public PageResponse<PrestudyPlaylistSummaryResponse> getMyPrestudyPlaylists(Long userId, int page, int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
		Page<PrestudyPlaylist> playlists = prestudyPlaylistRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

		Map<Long, Performance> performanceById = performanceRepository
			.findAllById(playlists.getContent().stream().map(PrestudyPlaylist::getPerformanceId).distinct().toList())
			.stream()
			.collect(Collectors.toMap(Performance::getPerformanceId, Function.identity()));

		List<PrestudyPlaylistSummaryResponse> content = playlists.getContent().stream()
			.map(p -> {
				Performance performance = performanceById.get(p.getPerformanceId());
				return PrestudyPlaylistSummaryResponse.of(
					p,
					performance == null ? null : performance.getPerformanceName(),
					performance == null ? null : performance.getPosterUrl()
				);
			})
			.toList();

		return PageResponse.of(content, playlists);
	}

	// 예습 플레이리스트 상세 조회
	public PrestudyPlaylistDetailResponse getPrestudyPlaylist(Long userId, Long prestudyPlaylistId) {
		PrestudyPlaylist playlist = findOwned(userId, prestudyPlaylistId);
		return PrestudyPlaylistDetailResponse.from(playlist);
	}

	// 예습 플레이리스트 트랙 조회
	public List<PrestudyPlaylistTrackResponse> getPrestudyPlaylistTracks(Long userId, Long prestudyPlaylistId) {
		findOwned(userId, prestudyPlaylistId);

		List<PrestudyPlaylistTrack> playlistTracks =
			prestudyPlaylistTrackRepository.findByPrestudyPlaylistIdOrderByTrackOrderAsc(prestudyPlaylistId);

		Map<Long, Track> trackById = trackRepository
			.findAllById(playlistTracks.stream().map(PrestudyPlaylistTrack::getTrackId).toList())
			.stream()
			.collect(Collectors.toMap(Track::getTrackId, Function.identity()));

		return playlistTracks.stream()
			.map(pt -> PrestudyPlaylistTrackResponse.of(pt, trackById.get(pt.getTrackId())))
			.toList();
	}

	// 예습 플레이리스트 후보 조회
	public PrestudyCandidateResponse getCandidates(Long userId, Long performanceId, Long analysisId) {
		performanceRepository.findByPerformanceIdAndIsDeletedFalse(performanceId)
			.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

		List<PerformanceArtist> lineupArtists =
			performanceArtistRepository.findByPerformanceIdOrderByLineupOrderAsc(performanceId);

		Map<Long, Artist> artistById = artistRepository
			.findAllById(lineupArtists.stream().map(PerformanceArtist::getArtistId).toList())
			.stream()
			.collect(Collectors.toMap(Artist::getArtistId, Function.identity()));

		Set<Long> analysisArtistIds = analysisArtistRepository
			.findByAnalysisIdAndIsExcludedFalse(analysisId).stream()
			.map(AnalysisArtist::getArtistId)
			.collect(Collectors.toSet());

		SpotifyAccount account = spotifyAccountRepository.findByUserId(userId)
			.filter(a -> a.getConnectionStatus() == ConnectionStatus.CONNECTED)
			.orElseThrow(() -> new BusinessException(ErrorCode.SPOTIFY_CONNECTION_REQUIRED));
		String accessToken = resolveAccessToken(account);

		List<PrestudyCandidateResponse.ArtistCandidate> artists = lineupArtists.stream()
			.map(la -> {
				Artist artist = artistById.get(la.getArtistId());
				boolean isFromOriginalPlaylist = analysisArtistIds.contains(la.getArtistId());

				List<PrestudyCandidateResponse.TrackCandidate> candidateTracks = isFromOriginalPlaylist
					? buildOriginalPlaylistTracks(artist.getArtistId())
					: buildTopTrackCandidate(accessToken, artist);

				return new PrestudyCandidateResponse.ArtistCandidate(
					artist.getArtistId(),
					artist.getArtistName(),
					isFromOriginalPlaylist,
					candidateTracks
				);
			})
			.toList();

		return new PrestudyCandidateResponse(performanceId, analysisId, artists);
	}

	/** 원본 플레이리스트에 있던 아티스트의 곡을 그대로 후보로 사용한다. */
	private List<PrestudyCandidateResponse.TrackCandidate> buildOriginalPlaylistTracks(Long artistId) {
		List<Long> trackIds = trackArtistRepository.findByArtistId(artistId).stream()
			.map(TrackArtist::getTrackId)
			.distinct()
			.toList();

		return trackRepository.findAllById(trackIds).stream()
			.map(track -> new PrestudyCandidateResponse.TrackCandidate(
				track.getTrackId(),
				track.getTrackName(),
				SourceType.ORIGINAL_PLAYLIST.name()
			))
			.toList();
	}

	/** 원본 플레이리스트에 없던 새 아티스트는 Spotify Top Track을 대표곡으로 사용한다. */
	private List<PrestudyCandidateResponse.TrackCandidate> buildTopTrackCandidate(String accessToken, Artist artist) {
		if (artist.getSpotifyArtistId() == null) {
			return List.of();
		}

		SpotifyTrackSnapshot topTrack = spotifyPlaylistClient.fetchTopTrack(accessToken, artist.getSpotifyArtistId());
		if (topTrack == null) {
			return List.of();
		}

		Track track = trackRepository.findBySpotifyTrackId(topTrack.spotifyTrackId())
			.orElseGet(() -> trackRepository.save(new Track(
				topTrack.spotifyTrackId(), topTrack.trackName(), topTrack.albumName(),
				topTrack.albumImageUrl(), topTrack.spotifyTrackUrl(), topTrack.previewUrl(),
				topTrack.durationMs(), topTrack.isPlayable()
			)));

		return List.of(new PrestudyCandidateResponse.TrackCandidate(
			track.getTrackId(),
			track.getTrackName(),
			SourceType.MATCHED_ARTIST.name()
		));
	}

	// 예습 플레이리스트 생성
	@Transactional
	public CreatePrestudyPlaylistResponse createPrestudyPlaylist(
		Long userId, Long performanceId, CreatePrestudyPlaylistRequest request
	) {
		performanceRepository.findByPerformanceIdAndIsDeletedFalse(performanceId)
			.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

		List<Track> tracks = trackRepository.findAllById(request.selectedTrackIds());
		if (tracks.size() != request.selectedTrackIds().size()) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
		}

		SpotifyAccount account = spotifyAccountRepository.findByUserId(userId)
			.filter(a -> a.getConnectionStatus() == ConnectionStatus.CONNECTED)
			.orElseThrow(() -> new BusinessException(ErrorCode.SPOTIFY_CONNECTION_REQUIRED));

		String accessToken = resolveAccessToken(account);

		PrestudyPlaylist playlist = new PrestudyPlaylist(
			request.playlistTitle(),
			request.isPublic(),
			userId,
			performanceId,
			request.analysisId()
		);
		playlist = prestudyPlaylistRepository.save(playlist);

		String spotifyPlaylistId;
		try {
			spotifyPlaylistId = spotifyPlaylistClient.createPlaylist(
				accessToken, account.getSpotifyUserId(), request.playlistTitle(), request.isPublic()
			);
			spotifyPlaylistClient.addTracks(
				accessToken, spotifyPlaylistId,
				tracks.stream().map(Track::getSpotifyTrackId).toList()
			);
		} catch (SpotifyPlaylistApiException exception) {
			playlist.markFailed();
			throw new BusinessException(ErrorCode.PRESTUDY_PLAYLIST_CREATION_FAILED);
		}

		int order = 1;
		for (Track track : tracks) {
			prestudyPlaylistTrackRepository.save(new PrestudyPlaylistTrack(
				playlist.getPrestudyPlaylistId(), track.getTrackId(), order++,
				SourceType.ORIGINAL_PLAYLIST, false
			));
		}

		playlist.markCompleted(spotifyPlaylistId, tracks.size());

		return new CreatePrestudyPlaylistResponse(
			playlist.getPrestudyPlaylistId(), spotifyPlaylistId, tracks.size()
		);
	}

	private String resolveAccessToken(SpotifyAccount account) {
		LocalDateTime now = LocalDateTime.now(clock);
		if (account.getTokenExpiresAt() != null && account.getTokenExpiresAt().isAfter(now.plusSeconds(30))) {
			return tokenCipher.decrypt(account.getAccessTokenEncrypted());
		}
		try {
			String refreshToken = tokenCipher.decrypt(account.getRefreshTokenEncrypted());
			SpotifyTokenResponse refreshed = spotifyOAuthClient.refreshAccessToken(refreshToken);
			return refreshed.accessToken();
		} catch (SpotifyApiException exception) {
			throw new BusinessException(ErrorCode.SPOTIFY_REAUTHENTICATION_REQUIRED);
		}
	}

	private PrestudyPlaylist findOwned(Long userId, Long prestudyPlaylistId) {
		return prestudyPlaylistRepository.findByPrestudyPlaylistIdAndUserId(prestudyPlaylistId, userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
	}
}