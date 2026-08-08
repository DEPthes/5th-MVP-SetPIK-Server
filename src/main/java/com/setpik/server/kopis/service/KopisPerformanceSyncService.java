package com.setpik.server.kopis.service;

import com.setpik.server.artist.domain.Artist;
import com.setpik.server.artist.domain.Genre;
import com.setpik.server.artist.repository.ArtistRepository;
import com.setpik.server.artist.repository.GenreRepository;
import com.setpik.server.common.exception.BusinessException;
import com.setpik.server.common.exception.ErrorCode;
import com.setpik.server.kopis.client.KopisClient;
import com.setpik.server.kopis.dto.KopisPerformanceDetail;
import com.setpik.server.kopis.dto.KopisSyncResponse;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KopisPerformanceSyncService {

	private static final int KOPIS_MAX_DAYS = 31;
	private static final int KOPIS_PAGE_SIZE = 100;
	private static final int MAX_SYNC_DAYS = 366;
	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	private final KopisClient kopisClient;
	private final VenueRepository venueRepository;
	private final PerformanceRepository performanceRepository;
	private final ArtistRepository artistRepository;
	private final GenreRepository genreRepository;
	private final PerformanceArtistRepository performanceArtistRepository;
	private final PerformanceGenreRepository performanceGenreRepository;

	public KopisPerformanceSyncService(
		KopisClient kopisClient,
		VenueRepository venueRepository,
		PerformanceRepository performanceRepository,
		ArtistRepository artistRepository,
		GenreRepository genreRepository,
		PerformanceArtistRepository performanceArtistRepository,
		PerformanceGenreRepository performanceGenreRepository
	) {
		this.kopisClient = kopisClient;
		this.venueRepository = venueRepository;
		this.performanceRepository = performanceRepository;
		this.artistRepository = artistRepository;
		this.genreRepository = genreRepository;
		this.performanceArtistRepository = performanceArtistRepository;
		this.performanceGenreRepository = performanceGenreRepository;
	}

	@Transactional
	public KopisSyncResponse sync(LocalDate fromDate, LocalDate toDate) {
		validateRange(fromDate, toDate);
		Set<String> performanceIds = collectPerformanceIds(fromDate, toDate);
		int createdCount = 0;
		int updatedCount = 0;
		LocalDateTime syncedAt = LocalDateTime.now(KST);

		for (String performanceId : performanceIds) {
			KopisPerformanceDetail detail = kopisClient.getPerformanceDetail(performanceId);
			boolean exists = performanceRepository.findByKopisPerformanceId(performanceId).isPresent();
			syncPerformance(detail, syncedAt);
			if (exists) updatedCount++; else createdCount++;
		}

		return new KopisSyncResponse(fromDate, toDate, performanceIds.size(), createdCount,
			updatedCount, OffsetDateTime.now(KST));
	}

	private Set<String> collectPerformanceIds(LocalDate fromDate, LocalDate toDate) {
		Set<String> ids = new LinkedHashSet<>();
		LocalDate chunkStart = fromDate;
		while (!chunkStart.isAfter(toDate)) {
			LocalDate chunkEnd = min(chunkStart.plusDays(KOPIS_MAX_DAYS - 1L), toDate);
			for (int page = 1; ; page++) {
				List<String> pageIds = kopisClient.getPerformanceIds(
					chunkStart, chunkEnd, page, KOPIS_PAGE_SIZE);
				ids.addAll(pageIds);
				if (pageIds.size() < KOPIS_PAGE_SIZE) break;
			}
			chunkStart = chunkEnd.plusDays(1);
		}
		return ids;
	}

	private void syncPerformance(KopisPerformanceDetail detail, LocalDateTime syncedAt) {
		Venue venue = upsertVenue(detail);
		Performance performance = performanceRepository
			.findByKopisPerformanceId(detail.kopisPerformanceId())
			.orElseGet(() -> new Performance(
				detail.kopisPerformanceId(), detail.performanceName(), detail.startDate(), detail.endDate(),
				detail.posterUrl(), detail.bookingUrl(), status(detail.status()), priceType(detail.priceText()),
				detail.priceText(), syncedAt, venue.getVenueId()));

		performance.syncFromKopis(detail.performanceName(), detail.startDate(), detail.endDate(),
			detail.posterUrl(), detail.bookingUrl(), status(detail.status()), priceType(detail.priceText()),
			detail.priceText(), syncedAt, venue.getVenueId());
		performance = performanceRepository.save(performance);

		replaceArtists(performance.getPerformanceId(), detail.artistNames());
		replaceGenre(performance.getPerformanceId(), detail.genreName());
	}

	private Venue upsertVenue(KopisPerformanceDetail detail) {
		String facilityId = detail.facilityId().isBlank()
			? "PERFORMANCE:" + detail.kopisPerformanceId()
			: detail.facilityId();
		KopisVenueDetail venueDetail = detail.facilityId().isBlank()
			? new KopisVenueDetail(facilityId, detail.venueName(), null, null, null)
			: kopisClient.getVenueDetail(detail.facilityId());
		String address = venueDetail.address();
		String city = firstNonBlank(detail.area(), addressPart(address, 0), "미상");
		String district = addressPart(address, 1);
		String venueName = firstNonBlank(venueDetail.venueName(), detail.venueName(), "미상");

		Venue venue = venueRepository.findByKopisVenueId(facilityId)
			.orElseGet(() -> new Venue(facilityId, venueName, city, district, address,
				venueDetail.latitude(), venueDetail.longitude()));
		venue.syncFromKopis(venueName, city, district, address,
			venueDetail.latitude(), venueDetail.longitude());
		return venueRepository.save(venue);
	}

	private void replaceArtists(Long performanceId, List<String> artistNames) {
		performanceArtistRepository.deleteByPerformanceId(performanceId);
		performanceArtistRepository.flush();
		long order = 1;
		for (String artistName : artistNames) {
			String normalizedName = Artist.normalize(artistName);
			if (normalizedName.isBlank()) continue;
			Artist artist = artistRepository.findByNormalizedName(normalizedName)
				.orElseGet(() -> artistRepository.save(Artist.fromKopis(artistName)));
			// KOPIS는 헤드라이너 여부를 제공하지 않으므로 임의로 true를 만들지 않는다.
			performanceArtistRepository.save(new PerformanceArtist(
				artist.getArtistId(), performanceId, order++, false));
		}
	}

	private void replaceGenre(Long performanceId, String genreName) {
		performanceGenreRepository.deleteByPerformanceId(performanceId);
		performanceGenreRepository.flush();
		if (genreName == null || genreName.isBlank()) return;
		String normalizedName = Artist.normalize(genreName);
		Genre genre = genreRepository.findByNormalizedName(normalizedName)
			.orElseGet(() -> genreRepository.save(new Genre(genreName, normalizedName)));
		performanceGenreRepository.save(new PerformanceGenre(performanceId, genre.getGenreId(), "KOPIS"));
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

	private void validateRange(LocalDate fromDate, LocalDate toDate) {
		if (fromDate == null || toDate == null || fromDate.isAfter(toDate)
			|| fromDate.plusDays(MAX_SYNC_DAYS - 1L).isBefore(toDate)) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}
	}

	private LocalDate min(LocalDate first, LocalDate second) {
		return first.isBefore(second) ? first : second;
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
