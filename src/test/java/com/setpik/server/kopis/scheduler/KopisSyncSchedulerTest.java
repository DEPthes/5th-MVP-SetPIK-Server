package com.setpik.server.kopis.scheduler;

import static org.mockito.Mockito.verify;

import com.setpik.server.kopis.config.KopisSyncProperties;
import com.setpik.server.kopis.service.KopisPerformanceSyncService;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KopisSyncSchedulerTest {

	@Mock
	private KopisPerformanceSyncService syncService;

	@Test
	void syncsOnlyTodayByDefault() {
		KopisSyncProperties properties = new KopisSyncProperties();
		KopisSyncScheduler scheduler = new KopisSyncScheduler(syncService, properties);
		LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

		scheduler.syncUpcomingPerformances();

		verify(syncService).sync(today, today);
	}
}
