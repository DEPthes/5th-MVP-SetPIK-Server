package com.setpik.server.performance.service;

import com.setpik.server.artist.domain.Artist;
import com.setpik.server.artist.repository.ArtistRepository;
import com.setpik.server.artist.domain.Genre;
import com.setpik.server.artist.repository.GenreRepository;
import com.setpik.server.performance.domain.PerformanceArtist;
import com.setpik.server.performance.domain.PerformanceGenre;
import com.setpik.server.performance.domain.PerformanceTag;
import com.setpik.server.performance.domain.PerformanceTagMap;
import com.setpik.server.performance.domain.PerformanceType;
import com.setpik.server.performance.domain.PerformanceTypeMap;
import com.setpik.server.performance.repository.PerformanceArtistRepository;
import com.setpik.server.performance.repository.PerformanceGenreRepository;
import com.setpik.server.performance.repository.PerformanceTagMapRepository;
import com.setpik.server.performance.repository.PerformanceTagRepository;
import com.setpik.server.performance.repository.PerformanceTypeMapRepository;
import com.setpik.server.performance.repository.PerformanceTypeRepository;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 공연 조회이력/관심공연 목록에서 공통으로 필요한 performanceType, artistNames를
 * performanceId 목록 단위로 일괄 조회한다(N+1 방지).
 */
@Service
@Transactional(readOnly = true)
public class PerformanceMetadataLookupService {

	private final PerformanceArtistRepository performanceArtistRepository;
	private final ArtistRepository artistRepository;
	private final PerformanceTypeMapRepository performanceTypeMapRepository;
	private final PerformanceTypeRepository performanceTypeRepository;
	private final PerformanceGenreRepository performanceGenreRepository;
	private final GenreRepository genreRepository;
	private final PerformanceTagMapRepository performanceTagMapRepository;
	private final PerformanceTagRepository performanceTagRepository;

	public PerformanceMetadataLookupService(
		PerformanceArtistRepository performanceArtistRepository,
		ArtistRepository artistRepository,
		PerformanceTypeMapRepository performanceTypeMapRepository,
		PerformanceTypeRepository performanceTypeRepository,
		PerformanceGenreRepository performanceGenreRepository,
		GenreRepository genreRepository,
		PerformanceTagMapRepository performanceTagMapRepository,
		PerformanceTagRepository performanceTagRepository
	) {
		this.performanceArtistRepository = performanceArtistRepository;
		this.artistRepository = artistRepository;
		this.performanceTypeMapRepository = performanceTypeMapRepository;
		this.performanceTypeRepository = performanceTypeRepository;
		this.performanceGenreRepository = performanceGenreRepository;
		this.genreRepository = genreRepository;
		this.performanceTagMapRepository = performanceTagMapRepository;
		this.performanceTagRepository = performanceTagRepository;
	}

	public Map<Long, List<String>> artistNamesByPerformanceId(Collection<Long> performanceIds) {
		if (performanceIds.isEmpty()) return Map.of();

		List<PerformanceArtist> lineups = performanceArtistRepository.findByPerformanceIdIn(List.copyOf(performanceIds));
		Map<Long, Artist> artistById = artistRepository
			.findAllById(lineups.stream().map(PerformanceArtist::getArtistId).distinct().toList())
			.stream()
			.collect(Collectors.toMap(Artist::getArtistId, Function.identity()));

		Map<Long, List<PerformanceArtist>> lineupsByPerformanceId = lineups.stream()
			.filter(lineup -> artistById.containsKey(lineup.getArtistId()))
			.collect(Collectors.groupingBy(PerformanceArtist::getPerformanceId));

		Map<Long, List<String>> result = new HashMap<>();
		lineupsByPerformanceId.forEach((performanceId, performanceLineups) -> result.put(
			performanceId,
			performanceLineups.stream()
				.sorted(Comparator.comparing(
					PerformanceArtist::getLineupOrder, Comparator.nullsLast(Comparator.naturalOrder())))
				.map(lineup -> artistById.get(lineup.getArtistId()).getArtistName())
				.toList()
		));
		return result;
	}

	public Map<Long, String> performanceTypeCodeByPerformanceId(Collection<Long> performanceIds) {
		if (performanceIds.isEmpty()) return Map.of();

		List<PerformanceTypeMap> mappings = performanceTypeMapRepository.findByPerformanceIdIn(List.copyOf(performanceIds));
		if (mappings.isEmpty()) return Map.of();

		Map<Long, String> typeCodeByTypeId = performanceTypeRepository
			.findAllById(mappings.stream().map(PerformanceTypeMap::getPerformanceTypeId).distinct().toList())
			.stream()
			.collect(Collectors.toMap(PerformanceType::getPerformanceTypeId, PerformanceType::getTypeCode));

		Map<Long, String> result = new HashMap<>();
		for (PerformanceTypeMap mapping : mappings) {
			result.putIfAbsent(mapping.getPerformanceId(), typeCodeByTypeId.get(mapping.getPerformanceTypeId()));
		}
		return result;
	}

	public Map<Long, String> genreNameByPerformanceId(Collection<Long> performanceIds) {
		if (performanceIds.isEmpty()) return Map.of();
		List<PerformanceGenre> mappings = performanceGenreRepository
			.findByPerformanceIdIn(List.copyOf(performanceIds)).stream()
			.filter(mapping -> "KOPIS".equals(mapping.getSourceType()))
			.toList();
		Map<Long, String> genreNameById = genreRepository
			.findAllById(mappings.stream().map(PerformanceGenre::getGenreId).distinct().toList())
			.stream()
			.collect(Collectors.toMap(Genre::getGenreId, Genre::getGenreName));
		Map<Long, String> result = new HashMap<>();
		for (PerformanceGenre mapping : mappings) {
			result.putIfAbsent(mapping.getPerformanceId(), genreNameById.get(mapping.getGenreId()));
		}
		return result;
	}

	public Map<Long, List<String>> tagCodesByPerformanceId(Collection<Long> performanceIds) {
		if (performanceIds.isEmpty()) return Map.of();
		List<PerformanceTagMap> mappings = performanceTagMapRepository
			.findByPerformanceIdIn(List.copyOf(performanceIds));
		Map<Long, String> tagCodeById = performanceTagRepository
			.findAllById(mappings.stream().map(PerformanceTagMap::getPerformanceTagId).distinct().toList())
			.stream()
			.collect(Collectors.toMap(PerformanceTag::getPerformanceTagId, PerformanceTag::getTagCode));
		return mappings.stream()
			.filter(mapping -> tagCodeById.containsKey(mapping.getPerformanceTagId()))
			.collect(Collectors.groupingBy(
				PerformanceTagMap::getPerformanceId,
				Collectors.mapping(mapping -> tagCodeById.get(mapping.getPerformanceTagId()),
					Collectors.collectingAndThen(Collectors.toList(), values -> values.stream().sorted().toList()))));
	}
}
