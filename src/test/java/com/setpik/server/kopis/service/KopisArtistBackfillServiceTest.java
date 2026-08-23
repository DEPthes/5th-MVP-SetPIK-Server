package com.setpik.server.kopis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.setpik.server.artist.domain.Artist;
import com.setpik.server.artist.repository.ArtistRepository;
import com.setpik.server.auth.client.SpotifyOAuthClient;
import com.setpik.server.kopis.dto.KopisArtistBackfillResponse;
import com.setpik.server.performance.repository.PerformanceArtistRepository;
import com.setpik.server.playlist.client.SpotifyPlaylistClient;
import com.setpik.server.playlist.client.dto.SpotifyArtistSearchResult;
import com.setpik.server.playlist.client.dto.SpotifyArtistSnapshot;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class KopisArtistBackfillServiceTest {

	@Mock private ArtistRepository artistRepository;
	@Mock private PerformanceArtistRepository performanceArtistRepository;
	@Mock private SpotifyOAuthClient spotifyOAuthClient;
	@Mock private SpotifyPlaylistClient spotifyPlaylistClient;

	@Test
	void remapsOnlyExistingKopisArtistToExistingSpotifyArtist() {
		Artist kopisArtist = Artist.fromKopis("백예린");
		ReflectionTestUtils.setField(kopisArtist, "artistId", 10L);
		Artist spotifyArtist = new Artist("spotify-1", "Yerin Baek", null);
		ReflectionTestUtils.setField(spotifyArtist, "artistId", 20L);
		when(artistRepository.findKopisOnlyArtistsInMusicPerformancesAfterId(
			org.mockito.ArgumentMatchers.eq(0L), org.mockito.ArgumentMatchers.any(Pageable.class)))
			.thenReturn(List.of(kopisArtist));
		when(spotifyOAuthClient.getClientCredentialsToken()).thenReturn("token");
		when(spotifyPlaylistClient.searchArtistByNameResult("token", "백예린"))
			.thenReturn(SpotifyArtistSearchResult.success(
				new SpotifyArtistSnapshot("spotify-1", "Yerin Baek", null)));
		when(artistRepository.findBySpotifyArtistId("spotify-1")).thenReturn(Optional.of(spotifyArtist));
		when(performanceArtistRepository.copyMusicPerformanceMappings(10L, 20L)).thenReturn(3);

		KopisArtistBackfillResponse result = service().backfill(null, 10);

		assertThat(result.candidateArtistCount()).isEqualTo(1);
		assertThat(result.matchedArtistCount()).isEqualTo(1);
		assertThat(result.remappedPerformanceArtistCount()).isEqualTo(3);
		assertThat(result.unmatchedArtistCount()).isZero();
		assertThat(result.nextAfterArtistId()).isEqualTo(10L);
		assertThat(result.retryRequired()).isFalse();
		assertThat(spotifyArtist.getKopisArtistId()).isEqualTo("백예린");
		verify(performanceArtistRepository).deleteMusicPerformanceMappings(10L);
	}

	@Test
	void leavesKopisArtistUntouchedWhenSpotifyArtistDoesNotExistLocally() {
		Artist kopisArtist = Artist.fromKopis("알수없는가수");
		ReflectionTestUtils.setField(kopisArtist, "artistId", 10L);
		when(artistRepository.findKopisOnlyArtistsInMusicPerformancesAfterId(
			org.mockito.ArgumentMatchers.eq(0L), org.mockito.ArgumentMatchers.any(Pageable.class)))
			.thenReturn(List.of(kopisArtist));
		when(spotifyOAuthClient.getClientCredentialsToken()).thenReturn("token");
		when(spotifyPlaylistClient.searchArtistByNameResult("token", "알수없는가수"))
			.thenReturn(SpotifyArtistSearchResult.success(
				new SpotifyArtistSnapshot("spotify-missing", "Unknown", null)));
		when(artistRepository.findBySpotifyArtistId("spotify-missing")).thenReturn(Optional.empty());

		KopisArtistBackfillResponse result = service().backfill(null, 10);

		assertThat(result.matchedArtistCount()).isZero();
		assertThat(result.unmatchedArtistCount()).isEqualTo(1);
		verify(performanceArtistRepository, org.mockito.Mockito.never())
			.copyMusicPerformanceMappings(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong());
	}

	@Test
	void keepsCursorWhenSpotifyApiRequestFails() {
		Artist firstArtist = Artist.fromKopis("가수A");
		ReflectionTestUtils.setField(firstArtist, "artistId", 11L);
		Artist failedArtist = Artist.fromKopis("가수B");
		ReflectionTestUtils.setField(failedArtist, "artistId", 12L);
		when(artistRepository.findKopisOnlyArtistsInMusicPerformancesAfterId(
			org.mockito.ArgumentMatchers.eq(0L), org.mockito.ArgumentMatchers.any(Pageable.class)))
			.thenReturn(List.of(firstArtist, failedArtist));
		when(spotifyOAuthClient.getClientCredentialsToken()).thenReturn("token");
		when(spotifyPlaylistClient.searchArtistByNameResult("token", "가수A"))
			.thenReturn(SpotifyArtistSearchResult.success(null));
		when(spotifyPlaylistClient.searchArtistByNameResult("token", "가수B"))
			.thenReturn(SpotifyArtistSearchResult.failure());

		KopisArtistBackfillResponse result = service().backfill(null, 10);

		assertThat(result.retryRequired()).isTrue();
		assertThat(result.nextAfterArtistId()).isNull();
		assertThat(result.unmatchedArtistCount()).isEqualTo(1);
	}

	private KopisArtistBackfillService service() {
		return new KopisArtistBackfillService(
			artistRepository, performanceArtistRepository, spotifyOAuthClient, spotifyPlaylistClient);
	}
}
