package com.setpik.server.kopis.service;

import com.setpik.server.common.exception.BusinessException;
import com.setpik.server.common.exception.ErrorCode;
import com.setpik.server.kopis.client.KopisClient;
import com.setpik.server.kopis.config.KopisApiProperties;
import com.setpik.server.kopis.dto.KopisPerformanceDetail;
import com.setpik.server.kopis.dto.KopisSyncResponse;
import com.setpik.server.kopis.dto.KopisVenueDetail;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class KopisPerformanceSyncService {

	private static final Logger log = LoggerFactory.getLogger(KopisPerformanceSyncService.class);
	private static final int KOPIS_MAX_DAYS = 31;
	private static final int KOPIS_PAGE_SIZE = 100;
	private static final int MAX_SYNC_DAYS = 366;
	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	private final KopisClient kopisClient;
	private final KopisPerformanceBatchWriter batchWriter;
	private final KopisApiProperties properties;
	private final AtomicBoolean syncRunning = new AtomicBoolean(false);

	public KopisPerformanceSyncService(
		KopisClient kopisClient,
		KopisPerformanceBatchWriter batchWriter,
		KopisApiProperties properties
	) {
		this.kopisClient = kopisClient;
		this.batchWriter = batchWriter;
		this.properties = properties;
	}

	/** 외부 조회는 병렬 처리하고 DB 저장은 짧은 배치 트랜잭션으로 나누어 수행한다. */
	public KopisSyncResponse sync(LocalDate fromDate, LocalDate toDate) {
		validateRange(fromDate, toDate);
		if (!syncRunning.compareAndSet(false, true)) {
			throw new BusinessException(ErrorCode.DUPLICATE_REQUEST);
		}

		try {
			return executeSync(fromDate, toDate);
		} finally {
			syncRunning.set(false);
		}
	}

	private KopisSyncResponse executeSync(LocalDate fromDate, LocalDate toDate) {
		Set<String> performanceIds = collectPerformanceIds(fromDate, toDate);
		ExecutorService executor = Executors.newFixedThreadPool(
			Math.max(1, properties.getDetailConcurrency())
		);
		try {
			FetchResult<KopisPerformanceDetail> details = fetchPerformanceDetails(performanceIds, executor);
			Map<String, KopisVenueDetail> venues = fetchVenueDetails(details.values(), executor);
			WriteSummary writeSummary = writeInBatches(details.values(), venues);
			int failedCount = details.failedCount() + writeSummary.failedCount();

			return new KopisSyncResponse(
				fromDate,
				toDate,
				performanceIds.size(),
				writeSummary.createdCount(),
				writeSummary.updatedCount(),
				failedCount,
				OffsetDateTime.now(KST)
			);
		} finally {
			executor.shutdown();
		}
	}

	private FetchResult<KopisPerformanceDetail> fetchPerformanceDetails(
		Set<String> performanceIds,
		ExecutorService executor
	) {
		List<Callable<KopisPerformanceDetail>> tasks = performanceIds.stream()
			.map(performanceId -> (Callable<KopisPerformanceDetail>) () -> {
				try {
					return kopisClient.getPerformanceDetail(performanceId);
				} catch (RuntimeException exception) {
					log.warn("KOPIS 공연 상세 조회를 건너뜁니다: performanceId={}", performanceId);
					return null;
				}
			})
			.toList();
		return invoke(tasks, executor);
	}

	private Map<String, KopisVenueDetail> fetchVenueDetails(
		List<KopisPerformanceDetail> details,
		ExecutorService executor
	) {
		Set<String> facilityIds = new LinkedHashSet<>();
		for (KopisPerformanceDetail detail : details) {
			if (detail.facilityId() != null && !detail.facilityId().isBlank()) {
				facilityIds.add(detail.facilityId());
			}
		}

		List<Callable<VenueFetchResult>> tasks = facilityIds.stream()
			.map(facilityId -> (Callable<VenueFetchResult>) () -> {
				try {
					return new VenueFetchResult(facilityId, kopisClient.getVenueDetail(facilityId));
				} catch (RuntimeException exception) {
					log.warn("KOPIS 공연장 상세 조회 실패, 공연 정보로 대체합니다: facilityId={}", facilityId);
					return null;
				}
			})
			.toList();
		FetchResult<VenueFetchResult> fetched = invoke(tasks, executor);
		Map<String, KopisVenueDetail> venues = new LinkedHashMap<>();
		for (VenueFetchResult result : fetched.values()) {
			venues.put(result.facilityId(), result.detail());
		}
		return venues;
	}

	private <T> FetchResult<T> invoke(List<Callable<T>> tasks, ExecutorService executor) {
		if (tasks.isEmpty()) {
			return new FetchResult<>(List.of(), 0);
		}
		try {
			List<Future<T>> futures = executor.invokeAll(tasks);
			List<T> values = new ArrayList<>();
			int failedCount = 0;
			for (Future<T> future : futures) {
				T value = future.get();
				if (value == null) failedCount++;
				else values.add(value);
			}
			return new FetchResult<>(values, failedCount);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
		} catch (Exception exception) {
			throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
		}
	}

	private WriteSummary writeInBatches(
		List<KopisPerformanceDetail> details,
		Map<String, KopisVenueDetail> venues
	) {
		int batchSize = Math.max(1, properties.getBatchSize());
		int createdCount = 0;
		int updatedCount = 0;
		int failedCount = 0;
		LocalDateTime syncedAt = LocalDateTime.now(KST);

		for (int start = 0; start < details.size(); start += batchSize) {
			List<KopisPerformanceDetail> batch = details.subList(
				start, Math.min(start + batchSize, details.size()));
			try {
				KopisBatchWriteResult result = batchWriter.writeBatch(batch, venues, syncedAt);
				createdCount += result.createdCount();
				updatedCount += result.updatedCount();
			} catch (RuntimeException exception) {
				log.warn("KOPIS 배치 저장 실패, 공연별 저장으로 재시도합니다: batchSize={}", batch.size());
				for (KopisPerformanceDetail detail : batch) {
					try {
						KopisBatchWriteResult result = batchWriter.writeBatch(List.of(detail), venues, syncedAt);
						createdCount += result.createdCount();
						updatedCount += result.updatedCount();
					} catch (RuntimeException itemException) {
						failedCount++;
						log.error("KOPIS 공연 저장을 건너뜁니다: performanceId={}",
							detail.kopisPerformanceId());
					}
				}
			}
		}
		return new WriteSummary(createdCount, updatedCount, failedCount);
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

	private void validateRange(LocalDate fromDate, LocalDate toDate) {
		if (fromDate == null || toDate == null || fromDate.isAfter(toDate)
			|| fromDate.plusDays(MAX_SYNC_DAYS - 1L).isBefore(toDate)) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}
	}

	private LocalDate min(LocalDate first, LocalDate second) {
		return first.isBefore(second) ? first : second;
	}

	private record FetchResult<T>(List<T> values, int failedCount) {
	}

	private record VenueFetchResult(String facilityId, KopisVenueDetail detail) {
	}

	private record WriteSummary(int createdCount, int updatedCount, int failedCount) {
	}
}
