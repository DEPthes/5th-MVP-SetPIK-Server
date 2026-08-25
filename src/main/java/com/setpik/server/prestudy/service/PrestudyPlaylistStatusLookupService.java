package com.setpik.server.prestudy.service;

import com.setpik.server.prestudy.domain.PrestudyPlaylist;
import com.setpik.server.prestudy.dto.PrestudyPlaylistCardStatus;
import com.setpik.server.prestudy.repository.PrestudyPlaylistRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PrestudyPlaylistStatusLookupService {

	private final PrestudyPlaylistRepository prestudyPlaylistRepository;

	public PrestudyPlaylistStatusLookupService(PrestudyPlaylistRepository prestudyPlaylistRepository) {
		this.prestudyPlaylistRepository = prestudyPlaylistRepository;
	}

	/** 현재 페이지의 공연들을 한 번에 조회하고 공연별 가장 최근 예습 플레이리스트만 반환한다. */
	public Map<Long, PrestudyPlaylistCardStatus> latestByPerformanceId(
		Long userId,
		List<Long> performanceIds
	) {
		if (performanceIds.isEmpty()) {
			return Map.of();
		}

		Map<Long, PrestudyPlaylistCardStatus> latestByPerformanceId = new LinkedHashMap<>();
		for (PrestudyPlaylist playlist : prestudyPlaylistRepository
			.findByUserIdAndPerformanceIdInAndSpotifyDeletedFalseOrderByCreatedAtDescPrestudyPlaylistIdDesc(
				userId, performanceIds)) {
			latestByPerformanceId.putIfAbsent(
				playlist.getPerformanceId(),
				PrestudyPlaylistCardStatus.from(playlist));
		}
		return latestByPerformanceId;
	}
}
