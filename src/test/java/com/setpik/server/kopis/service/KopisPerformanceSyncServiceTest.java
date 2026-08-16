package com.setpik.server.kopis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.setpik.server.common.exception.BusinessException;
import com.setpik.server.common.exception.ErrorCode;
import com.setpik.server.kopis.client.KopisClient;
import com.setpik.server.kopis.config.KopisApiProperties;
import com.setpik.server.kopis.dto.KopisPerformanceDetail;
import com.setpik.server.kopis.dto.KopisSyncResponse;
import com.setpik.server.kopis.dto.KopisVenueDetail;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KopisPerformanceSyncServiceTest {

	@Mock
	private KopisClient kopisClient;

	@Mock
	private KopisPerformanceBatchWriter batchWriter;

	private KopisPerformanceSyncService syncService;

	@BeforeEach
	void setUp() {
		KopisApiProperties properties = new KopisApiProperties();
		properties.setDetailConcurrency(2);
		properties.setBatchSize(50);
		syncService = new KopisPerformanceSyncService(kopisClient, batchWriter, properties);
	}

	@Test
	void fetchesSharedVenueOnceAndWritesDetailsAsBatch() {
		LocalDate date = LocalDate.of(2026, 8, 15);
		KopisPerformanceDetail first = detail("PF001", "FC001");
		KopisPerformanceDetail second = detail("PF002", "FC001");
		when(kopisClient.getPerformanceIds(date, date, 1, 100))
			.thenReturn(List.of("PF001", "PF002"));
		when(kopisClient.getPerformanceDetail("PF001")).thenReturn(first);
		when(kopisClient.getPerformanceDetail("PF002")).thenReturn(second);
		when(kopisClient.getVenueDetail("FC001")).thenReturn(venue("FC001"));
		when(batchWriter.writeBatch(anyList(), anyMap(), any(LocalDateTime.class)))
			.thenReturn(new KopisBatchWriteResult(2, 0));

		KopisSyncResponse response = syncService.sync(date, date);

		assertThat(response.fetchedPerformanceCount()).isEqualTo(2);
		assertThat(response.createdPerformanceCount()).isEqualTo(2);
		assertThat(response.failedPerformanceCount()).isZero();
		verify(kopisClient, times(1)).getVenueDetail("FC001");
		verify(batchWriter).writeBatch(anyList(), anyMap(), any(LocalDateTime.class));
	}

	@Test
	void skipsFailedDetailAndContinuesRemainingPerformances() {
		LocalDate date = LocalDate.of(2026, 8, 15);
		when(kopisClient.getPerformanceIds(date, date, 1, 100))
			.thenReturn(List.of("PF001", "PF002"));
		when(kopisClient.getPerformanceDetail("PF001")).thenReturn(detail("PF001", "FC001"));
		when(kopisClient.getPerformanceDetail("PF002"))
			.thenThrow(new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR));
		when(kopisClient.getVenueDetail("FC001")).thenReturn(venue("FC001"));
		when(batchWriter.writeBatch(anyList(), anyMap(), any(LocalDateTime.class)))
			.thenReturn(new KopisBatchWriteResult(1, 0));

		KopisSyncResponse response = syncService.sync(date, date);

		assertThat(response.createdPerformanceCount()).isEqualTo(1);
		assertThat(response.failedPerformanceCount()).isEqualTo(1);
	}

	private KopisPerformanceDetail detail(String performanceId, String facilityId) {
		LocalDate date = LocalDate.of(2026, 8, 15);
		return new KopisPerformanceDetail(
			performanceId,
			"SetPIK Festival " + performanceId,
			date,
			date.plusDays(2),
			"https://example.com/poster.jpg",
			"https://tickets.example.com/1",
			"공연예정",
			"전석 100,000원",
			"인천광역시",
			"대중음악",
			facilityId,
			"송도달빛축제공원",
			List.of("Artist A")
		);
	}

	private KopisVenueDetail venue(String facilityId) {
		return new KopisVenueDetail(
			facilityId,
			"송도달빛축제공원",
			"인천광역시 연수구 센트럴로 350",
			new BigDecimal("37.3921"),
			new BigDecimal("126.6399")
		);
	}
}
