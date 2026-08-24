package com.setpik.server.performance.service;

import com.setpik.server.analysis.repository.PlaylistAnalysisRepository;
import com.setpik.server.artist.domain.Artist;
import com.setpik.server.artist.domain.ArtistAliasResolutionStatus;
import com.setpik.server.artist.repository.ArtistAliasRepository;
import com.setpik.server.artist.repository.ArtistRepository;
import com.setpik.server.common.api.PageResponse;
import com.setpik.server.common.exception.BusinessException;
import com.setpik.server.common.exception.ErrorCode;
import com.setpik.server.performance.domain.Performance;
import com.setpik.server.performance.domain.PerformanceArtist;
import com.setpik.server.performance.domain.PerformanceMatch;
import com.setpik.server.performance.domain.PerformanceMatchArtist;
import com.setpik.server.performance.domain.Venue;
import com.setpik.server.performance.dto.MatchedArtistResponse;
import com.setpik.server.performance.dto.PerformanceDetailResponse;
import com.setpik.server.performance.dto.PerformanceRecommendationResponse;
import com.setpik.server.performance.dto.TicketScheduleResponse;
import com.setpik.server.performance.repository.PerformanceArtistRepository;
import com.setpik.server.performance.repository.PerformanceMatchArtistRepository;
import com.setpik.server.performance.repository.PerformanceMatchRepository;
import com.setpik.server.performance.repository.PerformanceRepository;
import com.setpik.server.performance.repository.TicketScheduleRepository;
import com.setpik.server.performance.repository.VenueRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class PerformanceService {
	private static final Set<String> RECOMMENDATION_SORT_FIELDS = Set.of(
		"matchPriority", "matchedArtistCount", "matchRatio", "calculatedAt"
	);

	private final PerformanceRepository performanceRepository;
	private final VenueRepository venueRepository;
	private final TicketScheduleRepository ticketScheduleRepository;
	private final PerformanceArtistRepository performanceArtistRepository;
	private final ArtistRepository artistRepository;
	private final ArtistAliasRepository artistAliasRepository;
	private final PerformanceMatchRepository performanceMatchRepository;
	private final PerformanceMatchArtistRepository performanceMatchArtistRepository;
	private final PlaylistAnalysisRepository playlistAnalysisRepository;

	public PerformanceService(
		PerformanceRepository performanceRepository,
		VenueRepository venueRepository,
		TicketScheduleRepository ticketScheduleRepository,
		PerformanceArtistRepository performanceArtistRepository,
		ArtistRepository artistRepository,
		ArtistAliasRepository artistAliasRepository,
		PerformanceMatchRepository performanceMatchRepository,
		PerformanceMatchArtistRepository performanceMatchArtistRepository,
		PlaylistAnalysisRepository playlistAnalysisRepository
	) {
		this.performanceRepository = performanceRepository;
		this.venueRepository = venueRepository;
		this.ticketScheduleRepository = ticketScheduleRepository;
		this.performanceArtistRepository = performanceArtistRepository;
		this.artistRepository = artistRepository;
		this.artistAliasRepository = artistAliasRepository;
		this.performanceMatchRepository = performanceMatchRepository;
		this.performanceMatchArtistRepository = performanceMatchArtistRepository;
		this.playlistAnalysisRepository = playlistAnalysisRepository;
	}

	public PerformanceDetailResponse getPerformance(Long performanceId) {
		Performance performance = performanceRepository.findByPerformanceIdAndIsDeletedFalse(performanceId)
			.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
		Venue venue = venueRepository.findById(performance.getVenueId())
			.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
		List<PerformanceArtist> lineup = performanceArtistRepository
			.findByPerformanceIdOrderByLineupOrderAsc(performanceId);
		Map<Long, Artist> artistById = artistRepository
			.findAllById(lineup.stream().map(PerformanceArtist::getArtistId).toList())
			.stream()
			.collect(Collectors.toMap(Artist::getArtistId, Function.identity()));
		Map<Long, String> aliasImageUrlByArtistId = aliasImageUrls(artistById.keySet());

		return PerformanceDetailResponse.of(performance, venue, lineup, artistById,
			aliasImageUrlByArtistId);
	}

	private Map<Long, String> aliasImageUrls(Set<Long> kopisArtistIds) {
		Map<Long, String> spotifyIdByKopisArtistId = artistAliasRepository
			.findByKopisArtistIdIn(kopisArtistIds).stream()
			.filter(alias -> alias.getResolutionStatus() == ArtistAliasResolutionStatus.RESOLVED)
			.filter(alias -> alias.getSpotifyArtistId() != null)
			.collect(Collectors.toMap(
				alias -> alias.getKopisArtistId(),
				alias -> alias.getSpotifyArtistId(),
				(left, right) -> left
			));
		if (spotifyIdByKopisArtistId.isEmpty()) return Map.of();

		Map<String, String> imageUrlBySpotifyId = artistRepository
			.findBySpotifyArtistIdIn(spotifyIdByKopisArtistId.values()).stream()
			.filter(artist -> artist.getImageUrl() != null && !artist.getImageUrl().isBlank())
			.collect(Collectors.toMap(
				Artist::getSpotifyArtistId,
				Artist::getImageUrl,
				(left, right) -> left
			));
		return spotifyIdByKopisArtistId.entrySet().stream()
			.filter(entry -> imageUrlBySpotifyId.containsKey(entry.getValue()))
			.collect(Collectors.toMap(Map.Entry::getKey,
				entry -> imageUrlBySpotifyId.get(entry.getValue())));
	}

	public List<TicketScheduleResponse> getTicketSchedules(Long performanceId) {
		ensurePerformanceExists(performanceId);

		return ticketScheduleRepository.findByPerformanceIdOrderByOpensAtAsc(performanceId).stream()
			.map(TicketScheduleResponse::from)
			.toList();
	}

	public PageResponse<PerformanceRecommendationResponse> getRecommendedPerformances(
		Long userId, Long analysisId, int page, int size, String sort
	) {
		if (!playlistAnalysisRepository.existsByAnalysisIdAndUserId(analysisId, userId)) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
		}
		Page<PerformanceMatch> matches;
		if ("matchPriority,asc".equals(sort)) {
			matches = performanceMatchRepository.findVisibleByAnalysisIdInRecommendationOrder(
				analysisId, PageRequest.of(page, size));
		} else {
			Pageable pageable = PageRequest.of(page, size, recommendationSort(sort));
			matches = performanceMatchRepository.findVisibleByAnalysisId(analysisId, pageable);
		}

		Map<Long, Performance> performanceById = performanceRepository
			.findAllById(matches.getContent().stream().map(PerformanceMatch::getPerformanceId).distinct().toList())
			.stream()
			.collect(Collectors.toMap(Performance::getPerformanceId, Function.identity()));

		List<PerformanceRecommendationResponse> content = matches.getContent().stream()
			.map(match -> {
				Performance performance = performanceById.get(match.getPerformanceId());
				if (performance == null) {
					throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
				}
				return PerformanceRecommendationResponse.of(match, performance.getPerformanceName());
			})
			.toList();

		return PageResponse.of(content, matches);
	}

	private Sort recommendationSort(String sort) {
		String[] parts = sort.split(",", -1);
		if (parts.length != 2 || !RECOMMENDATION_SORT_FIELDS.contains(parts[0])) {
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

	public List<MatchedArtistResponse> getMatchedArtists(
		Long userId,
		Long analysisId,
		Long performanceId
	) {
		if (!playlistAnalysisRepository.existsByAnalysisIdAndUserId(analysisId, userId)) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
		}
		ensurePerformanceExists(performanceId);
		PerformanceMatch match = performanceMatchRepository.findByAnalysisIdAndPerformanceId(analysisId, performanceId)
			.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

		List<PerformanceMatchArtist> matchArtists = performanceMatchArtistRepository.findByMatchId(match.getMatchId());

		Map<Long, Artist> artistById = artistRepository
			.findAllById(matchArtists.stream().map(PerformanceMatchArtist::getArtistId).toList())
			.stream()
			.collect(Collectors.toMap(Artist::getArtistId, Function.identity()));

		Map<Long, Boolean> headlinerByArtistId = performanceArtistRepository
			.findByPerformanceIdOrderByLineupOrderAsc(performanceId).stream()
			.collect(Collectors.toMap(PerformanceArtist::getArtistId, PerformanceArtist::getIsHeadliner));

		return matchArtists.stream()
			.map(matchArtist -> {
				Artist artist = artistById.get(matchArtist.getArtistId());
				if (artist == null) {
					throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
				}
				return MatchedArtistResponse.of(
					matchArtist,
					artist,
					headlinerByArtistId.getOrDefault(matchArtist.getArtistId(), false)
				);
			})
			.toList();
	}

	private void ensurePerformanceExists(Long performanceId) {
		if (!performanceRepository.existsByPerformanceIdAndIsDeletedFalse(performanceId)) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
		}
	}
}
