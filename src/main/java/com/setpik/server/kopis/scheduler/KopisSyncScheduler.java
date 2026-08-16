package com.setpik.server.kopis.scheduler;

import com.setpik.server.kopis.config.KopisSyncProperties;
import com.setpik.server.kopis.service.KopisPerformanceSyncService;
import java.time.LocalDate;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "kopis.sync", name = "enabled", havingValue = "true")
public class KopisSyncScheduler {

	private static final Logger log = LoggerFactory.getLogger(KopisSyncScheduler.class);
	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	private final KopisPerformanceSyncService syncService;
	private final KopisSyncProperties properties;

	public KopisSyncScheduler(
		KopisPerformanceSyncService syncService,
		KopisSyncProperties properties
	) {
		this.syncService = syncService;
		this.properties = properties;
	}

	@Scheduled(cron = "${kopis.sync.cron:0 0 3 * * *}", zone = "Asia/Seoul")
	public void syncUpcomingPerformances() {
		LocalDate fromDate = LocalDate.now(KST);
		LocalDate toDate = fromDate.plusDays(Math.max(1, properties.getFutureDays()) - 1L);
		try {
			syncService.sync(fromDate, toDate);
		} catch (RuntimeException exception) {
			log.error("KOPIS 자동 동기화에 실패했습니다: fromDate={}, toDate={}",
				fromDate, toDate, exception);
		}
	}
}
