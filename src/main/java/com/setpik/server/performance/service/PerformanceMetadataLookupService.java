package com.setpik.server.performance.service;

import com.setpik.server.artist.domain.Artist;
import com.setpik.server.artist.repository.ArtistRepository;
import com.setpik.server.performance.domain.PerformanceArtist;
import com.setpik.server.performance.domain.PerformanceType;
import com.setpik.server.performance.domain.PerformanceTypeMap;
import com.setpik.server.performance.repository.PerformanceArtistRepository;
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

	public PerformanceMetadataLookupService(
		PerformanceArtistRepository performanceArtistRepository,
		ArtistRepository artistRepository,
		PerformanceTypeMapRepository performanceTypeMapRepository,
		PerformanceTypeRepository performanceTypeRepository
	) {
		this.performanceArtistRepository = performanceArtistRepository;
		this.artistRepository = artistRepository;
		this.performanceTypeMapRepository = performanceTypeMapRepository;
		this.performanceTypeRepository = performanceTypeRepository;
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
}
