package com.setpik.server.artist.scheduler;

import com.setpik.server.artist.config.ArtistGenreSyncProperties;
import com.setpik.server.artist.service.ArtistGenreSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "artist-genre.sync", name = "enabled", havingValue = "true")
public class ArtistGenreSyncScheduler {
	private static final Logger log = LoggerFactory.getLogger(ArtistGenreSyncScheduler.class);
	private final ArtistGenreSyncService service;
	private final ArtistGenreSyncProperties properties;

	public ArtistGenreSyncScheduler(ArtistGenreSyncService service, ArtistGenreSyncProperties properties) {
		this.service = service;
		this.properties = properties;
	}

	@Scheduled(cron = "${artist-genre.sync.cron:30 */5 * * * *}", zone = "Asia/Seoul")
	public void syncPendingGenres() {
		try {
			var result = service.syncPendingGenres(properties.getBatchSize());
			log.info("아티스트 장르 동기화 완료: candidates={}, resolved={}, savedGenres={}, notFound={}, failed={}",
				result.candidateArtistCount(), result.resolvedArtistCount(), result.savedGenreCount(),
				result.notFoundCount(), result.failedCount());
		} catch (RuntimeException exception) {
			log.error("아티스트 장르 자동 동기화에 실패했습니다.", exception);
		}
	}
}
