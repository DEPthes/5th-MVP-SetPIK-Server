package com.setpik.server.kopis.service;

import com.setpik.server.artist.domain.Artist;
import com.setpik.server.artist.repository.ArtistRepository;
import com.setpik.server.auth.client.SpotifyOAuthClient;
import com.setpik.server.kopis.dto.KopisArtistBackfillResponse;
import com.setpik.server.performance.repository.PerformanceArtistRepository;
import com.setpik.server.playlist.client.SpotifyPlaylistClient;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

/** 이미 적재된 KOPIS 음악 공연 출연진을 기존 Spotify 아티스트와 연결한다. */
@Service
public class KopisArtistBackfillService {
	private static final Logger log = LoggerFactory.getLogger(KopisArtistBackfillService.class);

	private final ArtistRepository artistRepository;
	private final PerformanceArtistRepository performanceArtistRepository;
	private final SpotifyOAuthClient spotifyOAuthClient;
	private final SpotifyPlaylistClient spotifyPlaylistClient;

	public KopisArtistBackfillService(
		ArtistRepository artistRepository,
		PerformanceArtistRepository performanceArtistRepository,
		SpotifyOAuthClient spotifyOAuthClient,
		SpotifyPlaylistClient spotifyPlaylistClient
	) {
		this.artistRepository = artistRepository;
		this.performanceArtistRepository = performanceArtistRepository;
		this.spotifyOAuthClient = spotifyOAuthClient;
		this.spotifyPlaylistClient = spotifyPlaylistClient;
	}

	@Transactional
	public KopisArtistBackfillResponse backfill(Long afterArtistId, int requestedLimit) {
		int limit = Math.min(Math.max(requestedLimit, 1), 10);
		List<Artist> candidates = artistRepository
			.findKopisOnlyArtistsInMusicPerformancesAfterId(
				afterArtistId == null ? 0L : afterArtistId, PageRequest.of(0, limit));
		if (candidates.isEmpty()) {
			return new KopisArtistBackfillResponse(0, 0, 0, 0, null, false, LocalDateTime.now());
		}

		String accessToken;
		try {
			accessToken = spotifyOAuthClient.getClientCredentialsToken();
		} catch (RuntimeException exception) {
			log.warn("Spotify 클라이언트 자격 증명 토큰 발급 실패로 백필 배치를 재시도해야 합니다.", exception);
			return new KopisArtistBackfillResponse(
				candidates.size(), 0, 0, 0, null, true, LocalDateTime.now());
		}
		int matchedArtistCount = 0;
		int remappedPerformanceArtistCount = 0;
		int unmatchedArtistCount = 0;
		boolean retryRequired = false;

		for (Artist kopisArtist : candidates) {
			var searchResult = spotifyPlaylistClient
				.searchArtistByNameResult(accessToken, kopisArtist.getArtistName());
			if (!searchResult.requestSucceeded()) {
				retryRequired = true;
				break;
			}
			var searched = searchResult.artist();
			if (searched == null || searched.spotifyArtistId() == null) {
				unmatchedArtistCount++;
				continue;
			}

			var spotifyArtist = artistRepository.findBySpotifyArtistId(searched.spotifyArtistId());
			if (spotifyArtist.isEmpty()) {
				unmatchedArtistCount++;
				continue;
			}

			Artist targetArtist = spotifyArtist.get();
			int copiedCount = performanceArtistRepository.copyMusicPerformanceMappings(
				kopisArtist.getArtistId(), targetArtist.getArtistId());
			performanceArtistRepository.deleteMusicPerformanceMappings(kopisArtist.getArtistId());
			targetArtist.linkKopisArtistId(kopisArtist.getNormalizedName());
			matchedArtistCount++;
			remappedPerformanceArtistCount += copiedCount;
			log.info("KOPIS 출연진 Spotify 재연결 완료: kopisArtistId={}, spotifyArtistId={}, copiedMappings={}",
				kopisArtist.getArtistId(), targetArtist.getArtistId(), copiedCount);
		}

		return new KopisArtistBackfillResponse(
			candidates.size(), matchedArtistCount, remappedPerformanceArtistCount,
			unmatchedArtistCount,
			retryRequired ? null : candidates.get(candidates.size() - 1).getArtistId(),
			retryRequired,
			LocalDateTime.now());
	}
}
