package com.setpik.server.analysis.service;

import com.setpik.server.analysis.domain.AnalysisArtist;
import com.setpik.server.analysis.domain.AnalysisStatus;
import com.setpik.server.analysis.domain.PlaylistAnalysis;
import com.setpik.server.analysis.dto.AnalysisArtistResponse;
import com.setpik.server.analysis.dto.AnalysisArtistUpdateRequest;
import com.setpik.server.analysis.dto.AnalysisArtistUpdateResponse;
import com.setpik.server.analysis.dto.AnalysisDetailResponse;
import com.setpik.server.analysis.dto.AnalysisResponse;
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

	/** 분석 결과 조회 시 함께 내려줄 상위 아티스트 수. */
	private static final int TOP_ARTIST_LIMIT = 5;

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
		SpotifyPlaylist playlist = findOwnedPlaylist(userId, playlistId);

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
		}

		PlaylistAnalysis analysis = analysisRepository.save(new PlaylistAnalysis(
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
		findOwnedPlaylist(userId, playlistId);

		PlaylistAnalysis analysis = analysisRepository
			.findFirstByPlaylistIdAndUserIdOrderByAnalyzedAtDesc(playlistId, userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

		List<AnalysisArtist> topArtists = analysisArtistRepository
			.findByAnalysisIdOrderByDisplayRankAsc(analysis.getAnalysisId()).stream()
			.filter(artist -> !artist.getIsExcluded())
			.limit(TOP_ARTIST_LIMIT)
			.toList();

		return new AnalysisDetailResponse(
			analysis.getAnalysisId(),
			analysis.getAnalysisStatus(),
			analysis.getWarningMessage(),
			analysis.getSelectedArtistCount(),
			toArtistResponses(topArtists)
		);
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
		PlaylistAnalysis analysis = findOwnedAnalysis(userId, analysisId);

		Map<Long, AnalysisArtist> artistsById = analysisArtistRepository
			.findByAnalysisIdOrderByDisplayRankAsc(analysisId).stream()
			.collect(Collectors.toMap(AnalysisArtist::getArtistId, Function.identity()));

		int updatedCount = 0;
		for (AnalysisArtistUpdateRequest.ArtistExclusion exclusion : request.artists()) {
			AnalysisArtist target = artistsById.get(exclusion.artistId());
			if (target == null) {
				throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
			}
			if (!target.getIsExcluded().equals(exclusion.isExcluded())) {
				target.changeExcluded(exclusion.isExcluded());
				updatedCount++;
			}
		}

		long remainingCount = artistsById.values().stream()
			.filter(artist -> !artist.getIsExcluded())
			.count();
		analysis.updateSelectedArtistCount((int) remainingCount);

		return new AnalysisArtistUpdateResponse(analysisId, updatedCount);
	}

	/** 트랙 목록을 순회하며 아티스트별 출현 횟수를 집계한다. */
	private Map<Long, Integer> countArtistOccurrences(List<Long> trackIds) {
		if (trackIds.isEmpty()) {
			return Map.of();
		}

		Map<Long, Integer> occurrenceByArtistId = new LinkedHashMap<>();
		for (TrackArtist trackArtist : trackArtistRepository
			.findByTrackIdInOrderByTrackIdAscArtistOrderAsc(trackIds)) {
			occurrenceByArtistId.merge(trackArtist.getArtistId(), 1, Integer::sum);
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
			.sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
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
		analysisArtistRepository.saveAll(analysisArtists);
	}

	/** 분석 아티스트에 아티스트명을 채워 응답 DTO로 변환한다. */
	private List<AnalysisArtistResponse> toArtistResponses(List<AnalysisArtist> analysisArtists) {
		if (analysisArtists.isEmpty()) {
			return List.of();
		}

		List<Long> artistIds = analysisArtists.stream()
			.map(AnalysisArtist::getArtistId)
			.toList();

		Map<Long, String> namesById = artistRepository.findAllById(artistIds).stream()
			.collect(Collectors.toMap(Artist::getArtistId, Artist::getArtistName));

		return analysisArtists.stream()
			.map(artist -> AnalysisArtistResponse.of(
				artist, namesById.getOrDefault(artist.getArtistId(), "Unknown")))
			.toList();
	}

	private SpotifyPlaylist findOwnedPlaylist(Long userId, Long playlistId) {
		return playlistRepository
			.findByPlaylistIdAndUserIdAndDeletedAtIsNull(playlistId, userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
	}

	private PlaylistAnalysis findOwnedAnalysis(Long userId, Long analysisId) {
		return analysisRepository.findByAnalysisIdAndUserId(analysisId, userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
	}
}
