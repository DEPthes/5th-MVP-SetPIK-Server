package com.setpik.server.performance.service;

import com.setpik.server.analysis.domain.AnalysisArtist;
import com.setpik.server.analysis.domain.AnalysisStatus;
import com.setpik.server.analysis.domain.PlaylistAnalysis;
import com.setpik.server.analysis.repository.AnalysisArtistRepository;
import com.setpik.server.analysis.repository.PlaylistAnalysisRepository;
import com.setpik.server.artist.domain.Artist;
import com.setpik.server.artist.domain.ArtistGenre;
import com.setpik.server.artist.domain.Genre;
import com.setpik.server.artist.repository.ArtistGenreRepository;
import com.setpik.server.artist.repository.ArtistRepository;
import com.setpik.server.artist.repository.GenreRepository;
import com.setpik.server.common.exception.BusinessException;
import com.setpik.server.common.exception.ErrorCode;
import com.setpik.server.performance.domain.Performance;
import com.setpik.server.performance.domain.PerformanceArtist;
import com.setpik.server.performance.domain.PerformanceGenre;
import com.setpik.server.performance.domain.PerformanceMatch;
import com.setpik.server.performance.domain.PerformanceMatchArtist;
import com.setpik.server.performance.domain.PerformanceType;
import com.setpik.server.performance.domain.PerformanceTypeMap;
import com.setpik.server.performance.dto.PerformanceMatchRequest;
import com.setpik.server.performance.dto.PerformanceMatchResponse;
import com.setpik.server.performance.repository.PerformanceArtistRepository;
import com.setpik.server.performance.repository.PerformanceGenreRepository;
import com.setpik.server.performance.repository.PerformanceMatchArtistRepository;
import com.setpik.server.performance.repository.PerformanceMatchRepository;
import com.setpik.server.performance.repository.PerformanceRepository;
import com.setpik.server.performance.repository.PerformanceTypeMapRepository;
import com.setpik.server.performance.repository.PerformanceTypeRepository;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PerformanceMatchingService {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");
	private static final int SIMILAR_PERFORMANCE_LIMIT = 5;

	private final PlaylistAnalysisRepository playlistAnalysisRepository;
	private final AnalysisArtistRepository analysisArtistRepository;
	private final ArtistRepository artistRepository;
	private final ArtistGenreRepository artistGenreRepository;
	private final GenreRepository genreRepository;
	private final PerformanceRepository performanceRepository;
	private final PerformanceArtistRepository performanceArtistRepository;
	private final PerformanceGenreRepository performanceGenreRepository;
	private final PerformanceTypeMapRepository performanceTypeMapRepository;
	private final PerformanceTypeRepository performanceTypeRepository;
	private final PerformanceMatchRepository performanceMatchRepository;
	private final PerformanceMatchArtistRepository performanceMatchArtistRepository;

	public PerformanceMatchingService(
		PlaylistAnalysisRepository playlistAnalysisRepository,
		AnalysisArtistRepository analysisArtistRepository,
		ArtistRepository artistRepository,
		ArtistGenreRepository artistGenreRepository,
		GenreRepository genreRepository,
		PerformanceRepository performanceRepository,
		PerformanceArtistRepository performanceArtistRepository,
		PerformanceGenreRepository performanceGenreRepository,
		PerformanceTypeMapRepository performanceTypeMapRepository,
		PerformanceTypeRepository performanceTypeRepository,
		PerformanceMatchRepository performanceMatchRepository,
		PerformanceMatchArtistRepository performanceMatchArtistRepository
	) {
		this.playlistAnalysisRepository = playlistAnalysisRepository;
		this.analysisArtistRepository = analysisArtistRepository;
		this.artistRepository = artistRepository;
		this.artistGenreRepository = artistGenreRepository;
		this.genreRepository = genreRepository;
		this.performanceRepository = performanceRepository;
		this.performanceArtistRepository = performanceArtistRepository;
		this.performanceGenreRepository = performanceGenreRepository;
		this.performanceTypeMapRepository = performanceTypeMapRepository;
		this.performanceTypeRepository = performanceTypeRepository;
		this.performanceMatchRepository = performanceMatchRepository;
		this.performanceMatchArtistRepository = performanceMatchArtistRepository;
	}

	@Transactional
	public PerformanceMatchResponse calculate(
		Long userId,
		Long analysisId,
		PerformanceMatchRequest request
	) {
		validateDateRange(request);
		PlaylistAnalysis analysis = findCompletedOwnedAnalysis(userId, analysisId);

		try {
			List<AnalysisArtist> selectedArtists =
				analysisArtistRepository.findByAnalysisIdAndIsExcludedFalse(analysisId);
			if (selectedArtists.isEmpty()) {
				throw new BusinessException(ErrorCode.INVALID_REQUEST);
			}
			LocalDateTime calculatedAt = LocalDateTime.now(KST);
			List<MatchCandidate> candidates = buildCandidates(selectedArtists, request);

			deletePreviousMatches(analysisId);
			persistMatches(analysis.getAnalysisId(), candidates, calculatedAt);

			return new PerformanceMatchResponse(
				analysisId,
				candidates.size(),
				calculatedAt.atZone(KST).toOffsetDateTime()
			);
		} catch (RuntimeException exception) {
			if (exception instanceof BusinessException businessException) {
				throw businessException;
			}
			throw new BusinessException(ErrorCode.PERFORMANCE_MATCH_FAILED);
		}
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

	private void validateDateRange(PerformanceMatchRequest request) {
		if (request.fromDate() != null && request.toDate() != null
			&& request.fromDate().isAfter(request.toDate())) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}
	}

	private List<MatchCandidate> buildCandidates(
		List<AnalysisArtist> selectedArtists,
		PerformanceMatchRequest request
	) {
		if (selectedArtists.isEmpty()) {
			return List.of();
		}

		List<Performance> performances = performanceRepository
			.findMatchCandidates(request.fromDate(), request.toDate());
		if (performances.isEmpty()) {
			return List.of();
		}

		List<Long> performanceIds = performances.stream().map(Performance::getPerformanceId).toList();
		List<PerformanceArtist> lineup = performanceArtistRepository.findByPerformanceIdIn(performanceIds);
		Map<Long, Artist> artists = loadArtists(selectedArtists, lineup);
		Map<String, AnalysisArtist> selectedByName = selectedArtistsByNormalizedName(selectedArtists, artists);
		Map<Long, List<PerformanceArtist>> lineupByPerformance = lineup.stream()
			.collect(Collectors.groupingBy(PerformanceArtist::getPerformanceId));
		Set<Long> soloPerformanceIds = loadSoloPerformanceIds(performanceIds);

		// 이름 표기가 다른 Spotify/KOPIS 아티스트도 공백과 특수문자를 제거해 비교한다.
		List<MatchCandidate> directMatches = new ArrayList<>();
		Set<Long> directlyMatchedPerformanceIds = new HashSet<>();
		for (Performance performance : performances) {
			List<MatchedArtist> matchedArtists = matchArtists(
				lineupByPerformance.getOrDefault(performance.getPerformanceId(), List.of()),
				selectedByName,
				artists
			);
			if (matchedArtists.isEmpty()) {
				continue;
			}

			boolean solo = soloPerformanceIds.contains(performance.getPerformanceId());
			directMatches.add(directCandidate(performance, matchedArtists,
				lineupByPerformance.getOrDefault(performance.getPerformanceId(), List.of()).size(), solo));
			directlyMatchedPerformanceIds.add(performance.getPerformanceId());
		}
		directMatches.sort((left, right) -> {
			int priorityComparison = Integer.compare(left.priority(), right.priority());
			if (priorityComparison != 0) return priorityComparison;
			if (left.priority() == 2) {
				int artistCountComparison = Integer.compare(
					right.matchedArtists().size(), left.matchedArtists().size());
				if (artistCountComparison != 0) return artistCountComparison;
			}
			return left.performance().getStartDate().compareTo(right.performance().getStartDate());
		});

		// 직접 일치하지 않은 공연만 주요 아티스트의 장르를 기준으로 3순위에 포함한다.
		List<MatchCandidate> similarMatches = similarGenreCandidates(
			performances,
			directlyMatchedPerformanceIds,
			selectedArtists,
			lineupByPerformance
		);
		directMatches.addAll(similarMatches);
		return directMatches;
	}

	private Map<Long, Artist> loadArtists(
		List<AnalysisArtist> selectedArtists,
		List<PerformanceArtist> lineup
	) {
		Set<Long> artistIds = selectedArtists.stream()
			.map(AnalysisArtist::getArtistId)
			.collect(Collectors.toSet());
		lineup.stream().map(PerformanceArtist::getArtistId).forEach(artistIds::add);
		return artistRepository.findAllById(artistIds).stream()
			.collect(Collectors.toMap(Artist::getArtistId, Function.identity()));
	}

	private Map<String, AnalysisArtist> selectedArtistsByNormalizedName(
		List<AnalysisArtist> selectedArtists,
		Map<Long, Artist> artists
	) {
		Map<String, AnalysisArtist> selectedByName = new HashMap<>();
		for (AnalysisArtist selected : selectedArtists) {
			Artist artist = artists.get(selected.getArtistId());
			if (artist == null) {
				continue;
			}
			selectedByName.merge(
				normalizeArtistName(artist.getArtistName()),
				selected,
				(left, right) -> left.getOccurrenceCount() >= right.getOccurrenceCount() ? left : right
			);
		}
		return selectedByName;
	}

	private List<MatchedArtist> matchArtists(
		List<PerformanceArtist> lineup,
		Map<String, AnalysisArtist> selectedByName,
		Map<Long, Artist> artists
	) {
		Map<String, MatchedArtist> matches = new LinkedHashMap<>();
		for (PerformanceArtist performanceArtist : lineup) {
			Artist artist = artists.get(performanceArtist.getArtistId());
			if (artist == null) {
				continue;
			}
			String normalizedName = normalizeArtistName(artist.getArtistName());
			AnalysisArtist selected = selectedByName.get(normalizedName);
			if (selected != null) {
				matches.putIfAbsent(normalizedName, new MatchedArtist(
					performanceArtist.getArtistId(),
					artist.getArtistName(),
					selected.getOccurrenceCount()
				));
			}
		}
		return List.copyOf(matches.values());
	}

	private MatchCandidate directCandidate(
		Performance performance,
		List<MatchedArtist> matchedArtists,
		int lineupArtistCount,
		boolean solo
	) {
		byte priority = (byte) (solo ? 1 : 2);
		byte ratio = (byte) Math.min(100,
			Math.round(matchedArtists.size() * 100.0f / Math.max(1, lineupArtistCount)));
		String reason = solo
			? "플레이리스트에 " + representativeArtist(matchedArtists).artistName()
				+ "이(가) 포함되어 있으며, 해당 아티스트의 단독 공연이 예정되어 있습니다."
			: "플레이리스트 속 아티스트 " + matchedArtists.size() + "팀이 이 공연에 출연합니다.";
		return new MatchCandidate(
			performance, priority, ratio, null, reason, lineupArtistCount, matchedArtists);
	}

	private MatchedArtist representativeArtist(List<MatchedArtist> matchedArtists) {
		return matchedArtists.stream()
			.max(Comparator.comparingInt(MatchedArtist::occurrenceCount))
			.orElseThrow();
	}

	private Set<Long> loadSoloPerformanceIds(List<Long> performanceIds) {
		List<PerformanceTypeMap> mappings = performanceTypeMapRepository.findByPerformanceIdIn(performanceIds);
		Map<Long, PerformanceType> types = performanceTypeRepository.findAllById(
			mappings.stream().map(PerformanceTypeMap::getPerformanceTypeId).distinct().toList()
		).stream().collect(Collectors.toMap(PerformanceType::getPerformanceTypeId, Function.identity()));

		return mappings.stream()
			.filter(mapping -> isSoloType(types.get(mapping.getPerformanceTypeId())))
			.map(PerformanceTypeMap::getPerformanceId)
			.collect(Collectors.toSet());
	}

	private boolean isSoloType(PerformanceType type) {
		if (type == null) {
			return false;
		}
		String code = type.getTypeCode().toUpperCase(Locale.ROOT);
		return code.equals("SOLO") || code.equals("SOLO_CONCERT")
			|| type.getTypeName().contains("단독");
	}

	private List<MatchCandidate> similarGenreCandidates(
		List<Performance> performances,
		Set<Long> directlyMatchedPerformanceIds,
		List<AnalysisArtist> selectedArtists,
		Map<Long, List<PerformanceArtist>> lineupByPerformance
	) {
		List<Long> majorArtistIds = selectedArtists.stream()
			.filter(artist -> Boolean.TRUE.equals(artist.getIsMajor()))
			.map(AnalysisArtist::getArtistId)
			.toList();
		if (majorArtistIds.isEmpty()) {
			return List.of();
		}

		Set<Long> preferredGenreIds = artistGenreRepository.findByArtistIdIn(majorArtistIds).stream()
			.map(ArtistGenre::getGenreId)
			.collect(Collectors.toSet());
		if (preferredGenreIds.isEmpty()) {
			return List.of();
		}

		List<Long> unmatchedIds = performances.stream()
			.map(Performance::getPerformanceId)
			.filter(id -> !directlyMatchedPerformanceIds.contains(id))
			.toList();
		if (unmatchedIds.isEmpty()) {
			return List.of();
		}
		Map<Long, List<PerformanceGenre>> genresByPerformance = performanceGenreRepository
			.findByPerformanceIdIn(unmatchedIds).stream()
			.collect(Collectors.groupingBy(PerformanceGenre::getPerformanceId));
		List<Long> lineupArtistIds = unmatchedIds.stream()
			.flatMap(id -> lineupByPerformance.getOrDefault(id, List.of()).stream())
			.map(PerformanceArtist::getArtistId)
			.distinct()
			.toList();
		Map<Long, Set<Long>> genresByLineupArtist = lineupArtistIds.isEmpty()
			? Map.of()
			: artistGenreRepository.findByArtistIdIn(lineupArtistIds).stream()
				.collect(Collectors.groupingBy(
					ArtistGenre::getArtistId,
					Collectors.mapping(ArtistGenre::getGenreId, Collectors.toSet())
				));
		Map<Long, Genre> genres = genreRepository.findAllById(preferredGenreIds).stream()
			.collect(Collectors.toMap(Genre::getGenreId, Function.identity()));

		return performances.stream()
			.filter(performance -> !directlyMatchedPerformanceIds.contains(performance.getPerformanceId()))
			.map(performance -> similarGenreCandidate(
				performance,
				genresByPerformance.getOrDefault(performance.getPerformanceId(), List.of()),
				lineupByPerformance.getOrDefault(performance.getPerformanceId(), List.of()),
				genresByLineupArtist,
				preferredGenreIds,
				genres
			))
			.filter(java.util.Objects::nonNull)
			.sorted(Comparator.comparing(candidate -> candidate.performance().getStartDate()))
			.limit(SIMILAR_PERFORMANCE_LIMIT)
			.toList();
	}

	private MatchCandidate similarGenreCandidate(
		Performance performance,
		List<PerformanceGenre> performanceGenres,
		List<PerformanceArtist> lineup,
		Map<Long, Set<Long>> genresByLineupArtist,
		Set<Long> preferredGenreIds,
		Map<Long, Genre> genres
	) {
		Set<Long> candidateGenreIds = performanceGenres.stream()
			.map(PerformanceGenre::getGenreId)
			.collect(Collectors.toSet());
		lineup.stream()
			.flatMap(artist -> genresByLineupArtist
				.getOrDefault(artist.getArtistId(), Set.of()).stream())
			.forEach(candidateGenreIds::add);
		Long matchedGenreId = candidateGenreIds.stream()
			.filter(preferredGenreIds::contains)
			.findFirst()
			.orElse(null);
		if (matchedGenreId == null) {
			return null;
		}
		Genre genre = genres.get(matchedGenreId);
		String genreName = genre == null ? "유사 장르" : genre.getGenreName();
		return new MatchCandidate(
			performance,
			(byte) 3,
			null,
			matchedGenreId,
			"직접 포함된 아티스트는 없지만, " + genreName + " 공연이라 추천해요.",
			lineup.size(),
			List.of()
		);
	}

	private void deletePreviousMatches(Long analysisId) {
		List<Long> previousMatchIds = performanceMatchRepository.findAllByAnalysisId(analysisId).stream()
			.map(PerformanceMatch::getMatchId)
			.toList();
		if (!previousMatchIds.isEmpty()) {
			performanceMatchArtistRepository.deleteAllByMatchIdIn(previousMatchIds);
		}
		performanceMatchRepository.deleteAllByAnalysisId(analysisId);
	}

	private void persistMatches(
		Long analysisId,
		List<MatchCandidate> candidates,
		LocalDateTime calculatedAt
	) {
		for (MatchCandidate candidate : candidates) {
			PerformanceMatch match = performanceMatchRepository.saveAndFlush(PerformanceMatch.create(
				candidate.priority(),
				candidate.matchedArtists().size(),
				candidate.lineupArtistCount(),
				candidate.ratio(),
				candidate.reason(),
				calculatedAt,
				candidate.performance().getPerformanceId(),
				analysisId,
				candidate.genreId()
			));
			List<PerformanceMatchArtist> matchArtists = candidate.matchedArtists().stream()
				.map(artist -> PerformanceMatchArtist.create(
					match.getMatchId(), artist.artistId(), artist.occurrenceCount()))
				.toList();
			performanceMatchArtistRepository.saveAll(matchArtists);
		}
	}

	private String normalizeArtistName(String artistName) {
		String normalized = Normalizer.normalize(artistName, Normalizer.Form.NFKC)
			.toLowerCase(Locale.ROOT);
		return normalized.replaceAll("[\\p{Z}\\p{P}\\p{S}]", "");
	}

	private record MatchedArtist(Long artistId, String artistName, int occurrenceCount) {
	}

	private record MatchCandidate(
		Performance performance,
		byte priority,
		Byte ratio,
		Long genreId,
		String reason,
		int lineupArtistCount,
		List<MatchedArtist> matchedArtists
	) {
	}
}
