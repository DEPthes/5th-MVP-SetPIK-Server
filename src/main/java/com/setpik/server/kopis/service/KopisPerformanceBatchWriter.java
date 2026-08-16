package com.setpik.server.kopis.service;

import com.setpik.server.artist.domain.Artist;
import com.setpik.server.artist.domain.Genre;
import com.setpik.server.artist.repository.ArtistRepository;
import com.setpik.server.artist.repository.GenreRepository;
import com.setpik.server.kopis.dto.KopisPerformanceDetail;
import com.setpik.server.kopis.dto.KopisVenueDetail;
import com.setpik.server.performance.domain.Performance;
import com.setpik.server.performance.domain.PerformanceArtist;
import com.setpik.server.performance.domain.PerformanceGenre;
import com.setpik.server.performance.domain.PerformanceStatus;
import com.setpik.server.performance.domain.Venue;
import com.setpik.server.performance.repository.PerformanceArtistRepository;
import com.setpik.server.performance.repository.PerformanceGenreRepository;
import com.setpik.server.performance.repository.PerformanceRepository;
import com.setpik.server.performance.repository.VenueRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KopisPerformanceBatchWriter {

	private final VenueRepository venueRepository;
	private final PerformanceRepository performanceRepository;
	private final ArtistRepository artistRepository;
	private final GenreRepository genreRepository;
	private final PerformanceArtistRepository performanceArtistRepository;
	private final PerformanceGenreRepository performanceGenreRepository;

	public KopisPerformanceBatchWriter(
		VenueRepository venueRepository,
		PerformanceRepository performanceRepository,
		ArtistRepository artistRepository,
		GenreRepository genreRepository,
		PerformanceArtistRepository performanceArtistRepository,
		PerformanceGenreRepository performanceGenreRepository
	) {
		this.venueRepository = venueRepository;
		this.performanceRepository = performanceRepository;
		this.artistRepository = artistRepository;
		this.genreRepository = genreRepository;
		this.performanceArtistRepository = performanceArtistRepository;
		this.performanceGenreRepository = performanceGenreRepository;
	}

	/** 네트워크 호출 없이 전달받은 KOPIS 데이터를 한 번의 짧은 트랜잭션으로 저장한다. */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public KopisBatchWriteResult writeBatch(
		List<KopisPerformanceDetail> details,
		Map<String, KopisVenueDetail> venueDetails,
		LocalDateTime syncedAt
	) {
		if (details.isEmpty()) {
			return new KopisBatchWriteResult(0, 0);
		}

		Map<String, Venue> venues = upsertVenues(details, venueDetails);
		Map<String, Performance> existingPerformances = performanceRepository
			.findByKopisPerformanceIdIn(details.stream()
				.map(KopisPerformanceDetail::kopisPerformanceId).toList())
			.stream()
			.collect(Collectors.toMap(Performance::getKopisPerformanceId, Function.identity()));
		int updatedCount = existingPerformances.size();

		List<Performance> performances = new ArrayList<>();
		for (KopisPerformanceDetail detail : details) {
			Venue venue = venues.get(facilityId(detail));
			Performance performance = existingPerformances.get(detail.kopisPerformanceId());
			if (performance == null) {
				performance = new Performance(
					detail.kopisPerformanceId(), detail.performanceName(), detail.startDate(), detail.endDate(),
					detail.posterUrl(), detail.bookingUrl(), status(detail.status()), priceType(detail.priceText()),
					detail.priceText(), syncedAt, venue.getVenueId());
			}
			performance.syncFromKopis(
				detail.performanceName(), detail.startDate(), detail.endDate(), detail.posterUrl(),
				detail.bookingUrl(), status(detail.status()), priceType(detail.priceText()),
				detail.priceText(), syncedAt, venue.getVenueId());
			performances.add(performance);
		}
		performances = performanceRepository.saveAllAndFlush(performances);

		Map<String, Artist> artists = upsertArtists(details);
		Map<String, Genre> genres = upsertGenres(details);
		replaceMappings(details, performances, artists, genres);

		return new KopisBatchWriteResult(details.size() - updatedCount, updatedCount);
	}

	private Map<String, Venue> upsertVenues(
		List<KopisPerformanceDetail> details,
		Map<String, KopisVenueDetail> venueDetails
	) {
		Map<String, KopisPerformanceDetail> sourceByFacility = new LinkedHashMap<>();
		for (KopisPerformanceDetail detail : details) {
			sourceByFacility.putIfAbsent(facilityId(detail), detail);
		}
		Map<String, Venue> venues = venueRepository
			.findByKopisVenueIdIn(new ArrayList<>(sourceByFacility.keySet()))
			.stream()
			.collect(Collectors.toMap(Venue::getKopisVenueId, Function.identity()));

		for (Map.Entry<String, KopisPerformanceDetail> entry : sourceByFacility.entrySet()) {
			String facilityId = entry.getKey();
			KopisPerformanceDetail source = entry.getValue();
			KopisVenueDetail detail = venueDetails.get(facilityId);
			String address = detail == null ? null : detail.address();
			String venueName = firstNonBlank(
				detail == null ? null : detail.venueName(), source.venueName(), "미상");
			String city = firstNonBlank(source.area(), addressPart(address, 0), "미상");
			String district = addressPart(address, 1);
			Venue venue = venues.computeIfAbsent(facilityId, ignored -> new Venue(
				facilityId, venueName, city, district, address,
				detail == null ? null : detail.latitude(),
				detail == null ? null : detail.longitude()));
			venue.syncFromKopis(
				venueName, city, district, address,
				detail == null ? null : detail.latitude(),
				detail == null ? null : detail.longitude());
		}
		return venueRepository.saveAllAndFlush(venues.values()).stream()
			.collect(Collectors.toMap(Venue::getKopisVenueId, Function.identity()));
	}

	private Map<String, Artist> upsertArtists(List<KopisPerformanceDetail> details) {
		Map<String, String> names = new LinkedHashMap<>();
		for (KopisPerformanceDetail detail : details) {
			for (String artistName : detail.artistNames()) {
				String normalizedName = Artist.normalize(artistName);
				if (!normalizedName.isBlank()) names.putIfAbsent(normalizedName, artistName);
			}
		}
		if (names.isEmpty()) return Map.of();

		Map<String, Artist> artists = artistRepository
			.findByNormalizedNameIn(new ArrayList<>(names.keySet())).stream()
			.collect(Collectors.toMap(Artist::getNormalizedName, Function.identity()));
		List<Artist> created = names.entrySet().stream()
			.filter(entry -> !artists.containsKey(entry.getKey()))
			.map(entry -> Artist.fromKopis(entry.getValue()))
			.toList();
		artistRepository.saveAllAndFlush(created)
			.forEach(artist -> artists.put(artist.getNormalizedName(), artist));
		return artists;
	}

	private Map<String, Genre> upsertGenres(List<KopisPerformanceDetail> details) {
		Map<String, String> names = new LinkedHashMap<>();
		for (KopisPerformanceDetail detail : details) {
			if (detail.genreName() == null || detail.genreName().isBlank()) continue;
			String normalizedName = Artist.normalize(detail.genreName());
			names.putIfAbsent(normalizedName, detail.genreName());
		}
		if (names.isEmpty()) return Map.of();

		Map<String, Genre> genres = genreRepository
			.findByNormalizedNameIn(new ArrayList<>(names.keySet())).stream()
			.collect(Collectors.toMap(Genre::getNormalizedName, Function.identity()));
		List<Genre> created = names.entrySet().stream()
			.filter(entry -> !genres.containsKey(entry.getKey()))
			.map(entry -> new Genre(entry.getValue(), entry.getKey()))
			.toList();
		genreRepository.saveAllAndFlush(created)
			.forEach(genre -> genres.put(genre.getNormalizedName(), genre));
		return genres;
	}

	private void replaceMappings(
		List<KopisPerformanceDetail> details,
		List<Performance> performances,
		Map<String, Artist> artists,
		Map<String, Genre> genres
	) {
		Map<String, Performance> performanceByKopisId = performances.stream()
			.collect(Collectors.toMap(Performance::getKopisPerformanceId, Function.identity()));
		List<Long> performanceIds = performances.stream().map(Performance::getPerformanceId).toList();
		performanceArtistRepository.deleteByPerformanceIdIn(performanceIds);
		performanceGenreRepository.deleteByPerformanceIdIn(performanceIds);

		List<PerformanceArtist> artistMappings = new ArrayList<>();
		List<PerformanceGenre> genreMappings = new ArrayList<>();
		for (KopisPerformanceDetail detail : details) {
			Long performanceId = performanceByKopisId.get(detail.kopisPerformanceId()).getPerformanceId();
			Set<Long> mappedArtistIds = new LinkedHashSet<>();
			long order = 1;
			for (String artistName : detail.artistNames()) {
				Artist artist = artists.get(Artist.normalize(artistName));
				if (artist != null && mappedArtistIds.add(artist.getArtistId())) {
					artistMappings.add(new PerformanceArtist(
						artist.getArtistId(), performanceId, order++, false));
				}
			}
			if (detail.genreName() != null && !detail.genreName().isBlank()) {
				Genre genre = genres.get(Artist.normalize(detail.genreName()));
				if (genre != null) {
					genreMappings.add(new PerformanceGenre(performanceId, genre.getGenreId(), "KOPIS"));
				}
			}
		}
		performanceArtistRepository.saveAll(artistMappings);
		performanceGenreRepository.saveAll(genreMappings);
	}

	private String facilityId(KopisPerformanceDetail detail) {
		return detail.facilityId() == null || detail.facilityId().isBlank()
			? "PERFORMANCE:" + detail.kopisPerformanceId()
			: detail.facilityId();
	}

	private PerformanceStatus status(String value) {
		return switch (value == null ? "" : value.trim()) {
			case "공연예정" -> PerformanceStatus.SCHEDULED;
			case "공연중" -> PerformanceStatus.ON_SALE;
			case "공연완료" -> PerformanceStatus.ENDED;
			default -> PerformanceStatus.SCHEDULED;
		};
	}

	private String priceType(String priceText) {
		if (priceText == null || priceText.isBlank() || priceText.contains("미정")) return "UNKNOWN";
		return priceText.contains("무료") ? "FREE" : "PAID";
	}

	private String addressPart(String address, int index) {
		if (address == null || address.isBlank()) return null;
		String[] parts = address.trim().split("\\s+");
		return parts.length > index ? parts[index] : null;
	}

	private String firstNonBlank(String... values) {
		for (String value : values) {
			if (value != null && !value.isBlank()) return value;
		}
		return null;
	}
}
