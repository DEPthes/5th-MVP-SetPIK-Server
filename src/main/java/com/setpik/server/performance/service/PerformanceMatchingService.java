package com.setpik.server.performance.service;

import com.setpik.server.analysis.domain.AnalysisArtist;
import com.setpik.server.analysis.domain.AnalysisStatus;
import com.setpik.server.analysis.domain.PlaylistAnalysis;
import com.setpik.server.analysis.repository.AnalysisArtistRepository;
import com.setpik.server.analysis.repository.PlaylistAnalysisRepository;
import com.setpik.server.artist.domain.Artist;
import com.setpik.server.artist.domain.ArtistAlias;
import com.setpik.server.artist.domain.ArtistAliasResolutionStatus;
import com.setpik.server.artist.domain.ArtistGenre;
import com.setpik.server.artist.domain.Genre;
import com.setpik.server.artist.domain.SpotifyArtistNameAlias;
import com.setpik.server.artist.repository.ArtistGenreRepository;
import com.setpik.server.artist.repository.ArtistAliasRepository;
import com.setpik.server.artist.repository.ArtistRepository;
import com.setpik.server.artist.repository.GenreRepository;
import com.setpik.server.artist.repository.SpotifyArtistNameAliasRepository;
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
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
	private static final PerformanceGenreCompatibilityPolicy GENRE_COMPATIBILITY =
		new PerformanceGenreCompatibilityPolicy();

	private final PlaylistAnalysisRepository playlistAnalysisRepository;
	private final AnalysisArtistRepository analysisArtistRepository;
	private final ArtistRepository artistRepository;
	private final ArtistAliasRepository artistAliasRepository;
	private final ArtistGenreRepository artistGenreRepository;
	private final SpotifyArtistNameAliasRepository spotifyArtistNameAliasRepository;
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
		ArtistAliasRepository artistAliasRepository,
		ArtistGenreRepository artistGenreRepository,
		SpotifyArtistNameAliasRepository spotifyArtistNameAliasRepository,
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
		this.artistAliasRepository = artistAliasRepository;
		this.artistGenreRepository = artistGenreRepository;
		this.spotifyArtistNameAliasRepository = spotifyArtistNameAliasRepository;
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
		List<PerformanceGenre> performanceGenres = performanceGenreRepository.findByPerformanceIdIn(performanceIds);
		Map<Long, List<PerformanceGenre>> genresByPerformance = performanceGenres.stream()
			.collect(Collectors.groupingBy(PerformanceGenre::getPerformanceId));
		Set<Long> allArtistIds = artists.keySet();
		List<ArtistGenre> artistGenres = artistGenreRepository.findByArtistIdIn(new ArrayList<>(allArtistIds));
		Map<Long, Set<Long>> genreIdsByArtist = artistGenres.stream().collect(Collectors.groupingBy(
			ArtistGenre::getArtistId,
			Collectors.mapping(ArtistGenre::getGenreId, Collectors.toSet())
		));
		Set<Long> allGenreIds = new HashSet<>();
		artistGenres.stream().map(ArtistGenre::getGenreId).forEach(allGenreIds::add);
		performanceGenres.stream().map(PerformanceGenre::getGenreId).forEach(allGenreIds::add);
		Map<Long, Genre> genres = genreRepository.findAllById(allGenreIds).stream()
			.collect(Collectors.toMap(Genre::getGenreId, Function.identity()));
		Map<String, AnalysisArtist> selectedByName = selectedArtistsByNormalizedName(selectedArtists, artists);
		Map<String, AnalysisArtist> selectedBySpotifyId = selectedArtistsBySpotifyId(selectedArtists, artists);
		Map<Long, List<String>> titleNamesByArtist = titleNamesByArtist(selectedArtists, artists);
		Map<Long, String> spotifyAliasByKopisArtistId = verifiedSpotifyAliases(lineup);
		Map<Long, List<PerformanceArtist>> lineupByPerformance = lineup.stream()
			.collect(Collectors.groupingBy(PerformanceArtist::getPerformanceId));
		Set<Long> soloPerformanceIds = loadSoloPerformanceIds(performanceIds);

		// 이름 표기가 다른 Spotify/KOPIS 아티스트도 공백과 특수문자를 제거해 비교한다.
		List<MatchCandidate> directMatches = new ArrayList<>();
		Set<Long> directlyMatchedPerformanceIds = new HashSet<>();
		for (Performance performance : performances) {
			List<PerformanceArtist> performanceLineup = lineupByPerformance
				.getOrDefault(performance.getPerformanceId(), List.of());
			Set<String> performanceGenreNames = genreNames(
				genresByPerformance.getOrDefault(performance.getPerformanceId(), List.of()), genres);
			List<MatchedArtist> matchedArtists = matchArtists(
				performanceLineup,
				selectedByName,
				selectedBySpotifyId,
				spotifyAliasByKopisArtistId, artists, genreIdsByArtist, genres, performanceGenreNames
			);
			if (matchedArtists.isEmpty()) {
				matchedArtists = matchByTitle(performance, selectedArtists, artists,
					titleNamesByArtist, genreIdsByArtist, genres, performanceGenreNames);
				if (!matchedArtists.isEmpty()) {
					directMatches.add(directCandidate(performance, matchedArtists, 1, true));
					directlyMatchedPerformanceIds.add(performance.getPerformanceId());
					continue;
				}
			}
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
			lineupByPerformance,
			genresByPerformance,
			genreIdsByArtist,
			genres
		);
		directMatches.addAll(similarMatches);
		return directMatches;
	}

	private Map<Long, List<String>> titleNamesByArtist(
		List<AnalysisArtist> selectedArtists, Map<Long, Artist> artists) {
		List<Long> selectedIds = selectedArtists.stream().map(AnalysisArtist::getArtistId).toList();
		Map<Long, List<String>> verifiedAliases = spotifyArtistNameAliasRepository
			.findByArtistIdIn(selectedIds).stream()
			.collect(Collectors.groupingBy(
				SpotifyArtistNameAlias::getArtistId,
				Collectors.mapping(SpotifyArtistNameAlias::getAliasName, Collectors.toList())
			));
		Map<Long, List<String>> result = new HashMap<>();
		for (Long artistId : selectedIds) {
			Artist artist = artists.get(artistId);
			if (artist == null) continue;
			LinkedHashSet<String> names = new LinkedHashSet<>();
			names.add(artist.getArtistName());
			names.addAll(verifiedAliases.getOrDefault(artistId, List.of()));
			result.put(artistId, List.copyOf(names));
		}
		return result;
	}

	private List<MatchedArtist> matchByTitle(
		Performance performance,
		List<AnalysisArtist> selectedArtists,
		Map<Long, Artist> artists,
		Map<Long, List<String>> titleNamesByArtist,
		Map<Long, Set<Long>> genreIdsByArtist,
		Map<Long, Genre> genres,
		Set<String> performanceGenreNames
	) {
		String normalizedTitle = normalizeTitlePhrase(performance.getPerformanceName());
		if (normalizedTitle.isBlank()) return List.of();

		List<MatchedArtist> candidates = new ArrayList<>();
		for (AnalysisArtist selected : selectedArtists) {
			boolean titleMatched = titleNamesByArtist.getOrDefault(selected.getArtistId(), List.of()).stream()
				.map(this::normalizeTitlePhrase)
				.filter(name -> name.length() >= 2)
				.anyMatch(name -> containsExactPhrase(normalizedTitle, name));
			if (!titleMatched || !GENRE_COMPATIBILITY.allowsDirectMatch(
				genreNames(genreIdsByArtist.getOrDefault(selected.getArtistId(), Set.of()), genres),
				performanceGenreNames)) {
				continue;
			}
			Artist artist = artists.get(selected.getArtistId());
			if (artist != null) {
				candidates.add(new MatchedArtist(selected.getArtistId(), artist.getArtistName(),
					selected.getOccurrenceCount()));
			}
		}
		return candidates.size() == 1 ? List.of(candidates.get(0)) : List.of();
	}

	private boolean containsExactPhrase(String normalizedTitle, String normalizedName) {
		return (" " + normalizedTitle + " ").contains(" " + normalizedName + " ");
	}

	private String normalizeTitlePhrase(String value) {
		if (value == null) return "";
		return Normalizer.normalize(value, Normalizer.Form.NFKC)
			.toLowerCase(Locale.ROOT)
			.replaceAll("[^\\p{L}\\p{N}]+", " ")
			.trim()
			.replaceAll("\\s+", " ");
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

	private Map<String, AnalysisArtist> selectedArtistsBySpotifyId(
		List<AnalysisArtist> selectedArtists,
		Map<Long, Artist> artists
	) {
		Map<String, AnalysisArtist> selectedBySpotifyId = new HashMap<>();
		for (AnalysisArtist selected : selectedArtists) {
			Artist artist = artists.get(selected.getArtistId());
			if (artist == null || artist.getSpotifyArtistId() == null || artist.getSpotifyArtistId().isBlank()) {
				continue;
			}
			selectedBySpotifyId.put(artist.getSpotifyArtistId(), selected);
		}
		return selectedBySpotifyId;
	}

	private Map<Long, String> verifiedSpotifyAliases(List<PerformanceArtist> lineup) {
		List<Long> kopisArtistIds = lineup.stream().map(PerformanceArtist::getArtistId).distinct().toList();
		if (kopisArtistIds.isEmpty()) {
			return Map.of();
		}
		return artistAliasRepository.findByKopisArtistIdIn(kopisArtistIds).stream()
			.filter(alias -> alias.getResolutionStatus() == ArtistAliasResolutionStatus.RESOLVED)
			.filter(alias -> alias.getSpotifyArtistId() != null && !alias.getSpotifyArtistId().isBlank())
			.collect(Collectors.toMap(ArtistAlias::getKopisArtistId, ArtistAlias::getSpotifyArtistId));
	}

	private List<MatchedArtist> matchArtists(
		List<PerformanceArtist> lineup,
		Map<String, AnalysisArtist> selectedByName,
		Map<String, AnalysisArtist> selectedBySpotifyId,
		Map<Long, String> spotifyAliasByKopisArtistId,
		Map<Long, Artist> artists,
		Map<Long, Set<Long>> genreIdsByArtist,
		Map<Long, Genre> genres,
		Set<String> performanceGenreNames
	) {
		Map<String, MatchedArtist> matches = new LinkedHashMap<>();
		for (PerformanceArtist performanceArtist : lineup) {
			Artist artist = artists.get(performanceArtist.getArtistId());
			if (artist == null) {
				continue;
			}
			String normalizedName = normalizeArtistName(artist.getArtistName());
			AnalysisArtist selected = selectedByName.get(normalizedName);
			if (selected == null) {
				selected = selectedBySpotifyId.get(spotifyAliasByKopisArtistId.get(performanceArtist.getArtistId()));
			}
			if (selected != null && GENRE_COMPATIBILITY.allowsDirectMatch(
				genreNames(genreIdsByArtist.getOrDefault(selected.getArtistId(), Set.of()), genres),
				performanceGenreNames)) {
				Artist selectedArtist = artists.get(selected.getArtistId());
				matches.putIfAbsent(String.valueOf(selected.getArtistId()), new MatchedArtist(
					selected.getArtistId(),
					selectedArtist == null ? artist.getArtistName() : selectedArtist.getArtistName(),
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
		Map<Long, List<PerformanceArtist>> lineupByPerformance,
		Map<Long, List<PerformanceGenre>> genresByPerformance,
		Map<Long, Set<Long>> genresByLineupArtist,
		Map<Long, Genre> genres
	) {
		List<Long> majorArtistIds = selectedArtists.stream()
			.filter(artist -> Boolean.TRUE.equals(artist.getIsMajor()))
			.map(AnalysisArtist::getArtistId)
			.toList();
		if (majorArtistIds.isEmpty()) {
			return List.of();
		}

		Set<Long> preferredGenreIds = majorArtistIds.stream()
			.flatMap(id -> genresByLineupArtist.getOrDefault(id, Set.of()).stream())
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
		Set<String> performanceGenreNames = genreNames(performanceGenres, genres);
		Set<String> candidateGenreNames = new HashSet<>(
			GENRE_COMPATIBILITY.specificGenresFromKopis(performanceGenreNames));
		lineup.stream()
			.flatMap(artist -> genresByLineupArtist
				.getOrDefault(artist.getArtistId(), Set.of()).stream())
			.map(genres::get)
			.filter(java.util.Objects::nonNull)
			.map(Genre::getGenreName)
			.filter(genre -> GENRE_COMPATIBILITY.isCompatible(genre, performanceGenreNames))
			.forEach(candidateGenreNames::add);
		Long matchedGenreId = preferredGenreIds.stream()
			.filter(id -> genres.containsKey(id))
			.filter(id -> candidateGenreNames.contains(genres.get(id).getGenreName()))
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

	private Set<String> genreNames(Collection<Long> genreIds, Map<Long, Genre> genres) {
		return genreIds.stream().map(genres::get).filter(java.util.Objects::nonNull)
			.map(Genre::getGenreName).collect(Collectors.toSet());
	}

	private Set<String> genreNames(List<PerformanceGenre> performanceGenres, Map<Long, Genre> genres) {
		return performanceGenres.stream().map(PerformanceGenre::getGenreId).map(genres::get)
			.filter(java.util.Objects::nonNull).map(Genre::getGenreName).collect(Collectors.toSet());
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
