package com.setpik.server.performance.service;

import com.setpik.server.artist.domain.Artist;
import com.setpik.server.artist.repository.ArtistRepository;
import com.setpik.server.common.api.PageResponse;
import com.setpik.server.common.exception.BusinessException;
import com.setpik.server.common.exception.ErrorCode;
import com.setpik.server.performance.domain.Performance;
import com.setpik.server.performance.domain.PerformanceArtist;
import com.setpik.server.performance.domain.Venue;
import com.setpik.server.performance.dto.PerformanceArtistResponse;
import com.setpik.server.performance.dto.PerformanceDetailResponse;
import com.setpik.server.performance.dto.PerformanceSummaryResponse;
import com.setpik.server.performance.dto.TicketScheduleResponse;
import com.setpik.server.performance.repository.PerformanceArtistRepository;
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

@Service
@Transactional(readOnly = true)
public class PerformanceService {

	private final PerformanceRepository performanceRepository;
	private final VenueRepository venueRepository;
	private final TicketScheduleRepository ticketScheduleRepository;
	private final PerformanceArtistRepository performanceArtistRepository;
	private final ArtistRepository artistRepository;

	public PerformanceService(
		PerformanceRepository performanceRepository,
		VenueRepository venueRepository,
		TicketScheduleRepository ticketScheduleRepository,
		PerformanceArtistRepository performanceArtistRepository,
		ArtistRepository artistRepository
	) {
		this.performanceRepository = performanceRepository;
		this.venueRepository = venueRepository;
		this.ticketScheduleRepository = ticketScheduleRepository;
		this.performanceArtistRepository = performanceArtistRepository;
		this.artistRepository = artistRepository;
	}

	public PageResponse<PerformanceSummaryResponse> getPerformances(int page, int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "startDate"));
		Page<Performance> performances = performanceRepository.findByIsDeletedFalse(pageable);

		Map<Long, Venue> venueById = venueRepository
			.findAllById(performances.getContent().stream().map(Performance::getVenueId).distinct().toList())
			.stream()
			.collect(Collectors.toMap(Venue::getVenueId, Function.identity()));

		List<PerformanceSummaryResponse> content = performances.getContent().stream()
			.map(performance -> PerformanceSummaryResponse.of(performance, venueById.get(performance.getVenueId())))
			.toList();

		return PageResponse.of(content, performances);
	}

	public PerformanceDetailResponse getPerformance(Long performanceId) {
		Performance performance = performanceRepository.findByPerformanceIdAndIsDeletedFalse(performanceId)
			.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
		Venue venue = venueRepository.findById(performance.getVenueId())
			.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

		return PerformanceDetailResponse.of(performance, venue);
	}

	public List<TicketScheduleResponse> getTicketSchedules(Long performanceId) {
		ensurePerformanceExists(performanceId);

		return ticketScheduleRepository.findByPerformanceIdOrderByOpensAtAsc(performanceId).stream()
			.map(TicketScheduleResponse::from)
			.toList();
	}

	public List<PerformanceArtistResponse> getArtists(Long performanceId) {
		ensurePerformanceExists(performanceId);

		List<PerformanceArtist> performanceArtists =
			performanceArtistRepository.findByPerformanceIdOrderByLineupOrderAsc(performanceId);

		Map<Long, Artist> artistById = artistRepository
			.findAllById(performanceArtists.stream().map(PerformanceArtist::getArtistId).toList())
			.stream()
			.collect(Collectors.toMap(Artist::getArtistId, Function.identity()));

		return performanceArtists.stream()
			.map(performanceArtist -> PerformanceArtistResponse.of(performanceArtist, artistById.get(performanceArtist.getArtistId())))
			.toList();
	}

	private void ensurePerformanceExists(Long performanceId) {
		if (!performanceRepository.existsByPerformanceIdAndIsDeletedFalse(performanceId)) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
		}
	}
}