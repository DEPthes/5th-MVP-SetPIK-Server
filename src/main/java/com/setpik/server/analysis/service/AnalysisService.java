package com.setpik.server.analysis.service;

import com.setpik.server.analysis.domain.AnalysisArtist;
import com.setpik.server.analysis.domain.AnalysisStatus;
import com.setpik.server.analysis.domain.PlaylistAnalysis;
import com.setpik.server.analysis.dto.AnalysisArtistResponse;
import com.setpik.server.analysis.dto.AnalysisArtistUpdateRequest;
import com.setpik.server.analysis.dto.AnalysisArtistUpdateResponse;
import com.setpik.server.analysis.dto.AnalysisDetailResponse;
import com.setpik.server.analysis.dto.AnalysisResponse;
import com.setpik.server.analysis.dto.ArtistSelectionCompleteResponse;
import com.setpik.server.analysis.dto.TopArtistResponse;
import com.setpik.server.analysis.repository.AnalysisArtistRepository;
import com.setpik.server.analysis.repository.PlaylistAnalysisRepository;
import com.setpik.server.artist.domain.Artist;
import com.setpik.server.artist.repository.ArtistRepository;
import com.setpik.server.common.api.PageResponse;
import com.setpik.server.common.exception.BusinessException;
import com.setpik.server.common.exception.ErrorCode;
import com.setpik.server.playlist.domain.PlaylistTrack;
import com.setpik.server.playlist.domain.SpotifyPlaylist;
import com.setpik.server.playlist.domain.TrackArtist;
import com.setpik.server.playlist.repository.PlaylistTrackRepository;
import com.setpik.server.playlist.repository.SpotifyPlaylistRepository;
import com.setpik.server.playlist.repository.TrackArtistRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AnalysisService {

	/** 주요 아티스트로 표시할 최소 출현 횟수. */
	private static final int MAJOR_ARTIST_THRESHOLD = 2;

	private final PlaylistAnalysisRepository analysisRepository;
	private final AnalysisArtistRepository analysisArtistRepository;
	private final SpotifyPlaylistRepository playlistRepository;
	private final PlaylistTrackRepository playlistTrackRepository;
	private final TrackArtistRepository trackArtistRepository;
	private final ArtistRepository artistRepository;

	public AnalysisService(PlaylistAnalysisRepository analysisRepository,
						   AnalysisArtistRepository analysisArtistRepository,
						   SpotifyPlaylistRepository playlistRepository,
						   PlaylistTrackRepository playlistTrackRepository,
						   TrackArtistRepository trackArtistRepository,
						   ArtistRepository artistRepository) {
		this.analysisRepository = analysisRepository;
		this.analysisArtistRepository = analysisArtistRepository;
		this.playlistRepository = playlistRepository;
		this.playlistTrackRepository = playlistTrackRepository;
		this.trackArtistRepository = trackArtistRepository;
		this.artistRepository = artistRepository;
	}

	@Transactional
	public AnalysisResponse analyze(Long userId, Long playlistId) {
		if (playlistId == null || playlistId <= 0) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}
		SpotifyPlaylist playlist = findOwnedPlaylist(userId, playlistId);

		try {
			return executeAnalysis(userId, playlistId, playlist);
		} catch (BusinessException exception) {
			throw exception;
		} catch (RuntimeException exception) {
			throw new BusinessException(ErrorCode.ANALYSIS_FAILED);
		}
	}

	private AnalysisResponse executeAnalysis(Long userId, Long playlistId, SpotifyPlaylist playlist) {
		List<Long> trackIds = playlistTrackRepository
			.findByPlaylistIdOrderByTrackPositionAsc(playlistId).stream()
			.map(PlaylistTrack::getTrackId)
			.toList();

		Map<Long, Integer> occurrenceByArtistId = countArtistOccurrences(trackIds);

		String warningMessage = null;
		if (trackIds.isEmpty()) {
			warningMessage = "플레이리스트에 트랙이 없어 분석할 아티스트를 찾지 못했습니다.";
		} else if (occurrenceByArtistId.isEmpty()) {
			warningMessage = "트랙에 연결된 아티스트 정보가 없습니다.";
		} else if (trackIds.size() < 5) {
			warningMessage = "곡 수가 적어 분석 정확도가 낮을 수 있습니다.";
		}

		PlaylistAnalysis analysis = analysisRepository.saveAndFlush(new PlaylistAnalysis(
			userId,
			playlistId,
			playlist.getSpotifyPlaylistId(),
			playlist.getPlaylistName(),
			playlist.getCoverImageUrl(),
			trackIds.size(),
			occurrenceByArtistId.size(),
			AnalysisStatus.COMPLETED,
			warningMessage
		));

		saveAnalysisArtists(analysis.getAnalysisId(), occurrenceByArtistId);

		return AnalysisResponse.from(analysis);
	}

	public AnalysisDetailResponse getLatestAnalysis(Long userId, Long playlistId) {
		if (playlistId == null || playlistId <= 0) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}
		findOwnedPlaylist(userId, playlistId);

		PlaylistAnalysis analysis = analysisRepository
			.findFirstByPlaylistIdAndUserIdOrderByAnalyzedAtDescAnalysisIdDesc(playlistId, userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

		List<AnalysisArtist> topArtists = analysisArtistRepository
			.findByAnalysisIdOrderByDisplayRankAsc(analysis.getAnalysisId()).stream()
			.filter(artist -> !artist.getIsExcluded())
			.toList();

		return new AnalysisDetailResponse(
			analysis.getAnalysisId(),
			analysis.getAnalysisStatus(),
			analysis.getWarningMessage(),
			analysis.getSelectedArtistCount(),
			toTopArtistResponses(topArtists)
		);
	}

	private List<TopArtistResponse> toTopArtistResponses(List<AnalysisArtist> analysisArtists) {
		if (analysisArtists.isEmpty()) {
			return List.of();
		}

		Map<Long, Artist> artistsById = artistRepository
			.findAllById(analysisArtists.stream().map(AnalysisArtist::getArtistId).toList()).stream()
			.collect(Collectors.toMap(Artist::getArtistId, Function.identity()));

		return analysisArtists.stream()
			.map(analysisArtist -> {
				Artist artist = artistsById.get(analysisArtist.getArtistId());
				return TopArtistResponse.of(
					analysisArtist,
					artist == null ? "Unknown" : artist.getArtistName(),
					artist == null ? null : artist.getImageUrl()
				);
			})
			.toList();
	}

	public PageResponse<AnalysisArtistResponse> getAnalysisArtists(
		Long userId, Long analysisId, boolean includeExcluded, Pageable pageable) {
		findOwnedAnalysis(userId, analysisId);

		Page<AnalysisArtist> page = includeExcluded
			? analysisArtistRepository.findByAnalysisId(analysisId, pageable)
			: analysisArtistRepository.findByAnalysisIdAndIsExcludedFalse(analysisId, pageable);

		return PageResponse.of(toArtistResponses(page.getContent()), page);
	}

	@Transactional
	public AnalysisArtistUpdateResponse updateArtistExclusion(
		Long userId, Long analysisId, AnalysisArtistUpdateRequest request) {
		if (analysisId == null || analysisId <= 0) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}
		Set<Long> requestedArtistIds = new HashSet<>();
		for (AnalysisArtistUpdateRequest.ArtistExclusion exclusion : request.artists()) {
			if (!requestedArtistIds.add(exclusion.artistId())) {
				throw new BusinessException(ErrorCode.DUPLICATE_REQUEST);
			}
		}

		PlaylistAnalysis analysis = findOwnedAnalysis(userId, analysisId);

		Map<Long, AnalysisArtist> artistsById = analysisArtistRepository
			.findByAnalysisIdOrderByDisplayRankAsc(analysisId).stream()
			.collect(Collectors.toMap(AnalysisArtist::getArtistId, Function.identity()));

		for (AnalysisArtistUpdateRequest.ArtistExclusion exclusion : request.artists()) {
			AnalysisArtist target = artistsById.get(exclusion.artistId());
			if (target == null) {
				throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
			}
			if (!target.getIsExcluded().equals(exclusion.isExcluded())) {
				target.changeExcluded(exclusion.isExcluded());
			}
		}

		long remainingCount = artistsById.values().stream()
			.filter(artist -> !artist.getIsExcluded())
			.count();
		analysis.updateSelectedArtistCount((int) remainingCount);

		return new AnalysisArtistUpdateResponse(analysisId, request.artists().size());
	}

	@Transactional
	public ArtistSelectionCompleteResponse completeArtistSelection(Long userId, Long analysisId) {
		PlaylistAnalysis analysis = findOwnedAnalysis(userId, analysisId);
		analysis.completeArtistSelection();
		return ArtistSelectionCompleteResponse.of(
			analysis.getAnalysisId(), analysis.getArtistSelectionCompletedAt());
	}

	/** 트랙 목록을 순회하며 아티스트별 출현 횟수를 집계한다. */
	private Map<Long, Integer> countArtistOccurrences(List<Long> trackIds) {
		if (trackIds.isEmpty()) {
			return Map.of();
		}

		Map<Long, Integer> occurrenceByTrackId = trackIds.stream()
			.collect(Collectors.toMap(Function.identity(), ignored -> 1, Integer::sum));
		Map<Long, Integer> occurrenceByArtistId = new LinkedHashMap<>();
		for (TrackArtist trackArtist : trackArtistRepository
			.findByTrackIdInOrderByTrackIdAscArtistOrderAsc(trackIds)) {
			int trackOccurrence = occurrenceByTrackId.getOrDefault(trackArtist.getTrackId(), 0);
			occurrenceByArtistId.merge(trackArtist.getArtistId(), trackOccurrence, Integer::sum);
		}
		return occurrenceByArtistId;
	}

	/** 출현 횟수 내림차순으로 순위를 매겨 분석 아티스트를 저장한다. */
	private void saveAnalysisArtists(Long analysisId, Map<Long, Integer> occurrenceByArtistId) {
		if (occurrenceByArtistId.isEmpty()) {
			return;
		}

		Map<Long, Artist> artistsById = artistRepository
			.findAllById(occurrenceByArtistId.keySet()).stream()
			.collect(Collectors.toMap(Artist::getArtistId, Function.identity()));

		List<Map.Entry<Long, Integer>> ranked = occurrenceByArtistId.entrySet().stream()
			.sorted(Comparator
				.<Map.Entry<Long, Integer>>comparingInt(Map.Entry::getValue).reversed()
				.thenComparing(
					entry -> popularityOf(artistsById.get(entry.getKey())),
					Comparator.nullsLast(Comparator.reverseOrder())
				)
				.thenComparing(Map.Entry::getKey))
			.toList();

		List<AnalysisArtist> analysisArtists = new ArrayList<>();
		int rank = 1;
		for (Map.Entry<Long, Integer> entry : ranked) {
			Artist artist = artistsById.get(entry.getKey());
			analysisArtists.add(new AnalysisArtist(
				analysisId,
				entry.getKey(),
				entry.getValue(),
				artist == null ? null : artist.getPopularity(),
				entry.getValue() >= MAJOR_ARTIST_THRESHOLD,
				rank++
			));
		}
		analysisArtistRepository.saveAllAndFlush(analysisArtists);
	}

	private Short popularityOf(Artist artist) {
		return artist == null ? null : artist.getPopularity();
	}

	/** 분석 아티스트에 아티스트명을 채워 응답 DTO로 변환한다. */
	private List<AnalysisArtistResponse> toArtistResponses(List<AnalysisArtist> analysisArtists) {
		if (analysisArtists.isEmpty()) {
			return List.of();
		}

		List<Long> artistIds = analysisArtists.stream()
			.map(AnalysisArtist::getArtistId)
			.toList();

		Map<Long, Artist> artistsById = artistRepository.findAllById(artistIds).stream()
			.collect(Collectors.toMap(Artist::getArtistId, Function.identity()));

		return analysisArtists.stream()
			.map(analysisArtist -> {
				Artist artist = artistsById.get(analysisArtist.getArtistId());
				return AnalysisArtistResponse.of(
					analysisArtist,
					artist == null ? "Unknown" : artist.getArtistName(),
					artist == null ? null : artist.getImageUrl()
				);
			})
			.toList();
	}

	private SpotifyPlaylist findOwnedPlaylist(Long userId, Long playlistId) {
		return playlistRepository
			.findByPlaylistIdAndUserIdAndDeletedAtIsNull(playlistId, userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
	}

	private PlaylistAnalysis findOwnedAnalysis(Long userId, Long analysisId) {
		if (analysisId == null || analysisId <= 0) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}
		return analysisRepository.findByAnalysisIdAndUserId(analysisId, userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
	}
}
