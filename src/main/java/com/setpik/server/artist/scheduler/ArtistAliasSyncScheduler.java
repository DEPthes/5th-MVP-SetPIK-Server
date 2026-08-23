package com.setpik.server.artist.scheduler;

import com.setpik.server.artist.config.ArtistAliasSyncProperties;
import com.setpik.server.artist.service.ArtistAliasSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 새 KOPIS 음악 출연진의 Alias를 작은 단위로 자동 확인한다. */
@Component
@ConditionalOnProperty(prefix = "artist-alias.sync", name = "enabled", havingValue = "true")
public class ArtistAliasSyncScheduler {

	private static final Logger log = LoggerFactory.getLogger(ArtistAliasSyncScheduler.class);
	private final ArtistAliasSyncService syncService;
	private final ArtistAliasSyncProperties properties;

	public ArtistAliasSyncScheduler(ArtistAliasSyncService syncService, ArtistAliasSyncProperties properties) {
		this.syncService = syncService;
		this.properties = properties;
	}

	@Scheduled(cron = "${artist-alias.sync.cron:0 */5 * * * *}", zone = "Asia/Seoul")
	public void syncPendingAliases() {
		try {
			syncService.syncPendingAliases(properties.getBatchSize());
		} catch (RuntimeException exception) {
			log.error("검증된 KOPIS 출연진 Alias 자동 동기화에 실패했습니다.", exception);
		}
	}
}
