package com.setpik.server.artist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.setpik.server.artist.client.WikidataArtistAliasClient;
import com.setpik.server.artist.domain.Artist;
import com.setpik.server.artist.domain.ArtistAlias;
import com.setpik.server.artist.domain.ArtistAliasResolutionStatus;
import com.setpik.server.artist.repository.ArtistAliasRepository;
import com.setpik.server.artist.repository.ArtistRepository;
import com.setpik.server.artist.repository.SpotifyArtistAliasSyncStatusRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ArtistAliasSyncServiceTest {

	@Test
	void resolvesKopisArtistFromSpotifyIdAndKoreanWikidataAliases() {
		ArtistRepository artists = mock(ArtistRepository.class);
		ArtistAliasRepository aliases = mock(ArtistAliasRepository.class);
		SpotifyArtistAliasSyncStatusRepository statuses =
			mock(SpotifyArtistAliasSyncStatusRepository.class);
		WikidataArtistAliasClient client = mock(WikidataArtistAliasClient.class);
		Artist spotifyArtist = mock(Artist.class);
		Artist kopisArtist = mock(Artist.class);

		when(spotifyArtist.getArtistId()).thenReturn(1L);
		when(spotifyArtist.getSpotifyArtistId()).thenReturn("24nUVBIlCGi4twz4nYxJum");
		when(kopisArtist.getArtistId()).thenReturn(2L);
		when(kopisArtist.getSpotifyAvailable()).thenReturn(false);
		when(artists.findPendingReverseAliasSyncArtistIds(100)).thenReturn(List.of(1L));
		when(artists.findAllById(List.of(1L))).thenReturn(List.of(spotifyArtist));
		when(client.resolveBySpotifyIds(List.of("24nUVBIlCGi4twz4nYxJum"))).thenReturn(Map.of(
			"24nUVBIlCGi4twz4nYxJum",
			WikidataArtistAliasClient.ReverseLookupResult.resolved(
				"Q41398331", List.of("프로미스", "프로미스9", "프로미스나인"))));
		when(artists.findByNormalizedNameIn(List.of("프로미스", "프로미스9", "프로미스나인")))
			.thenReturn(List.of(kopisArtist));
		when(aliases.findById(2L)).thenReturn(Optional.empty());
		when(artists.findUnresolvedKopisMusicArtistIds("음악", 99)).thenReturn(List.of());

		ArtistAliasSyncService service = new ArtistAliasSyncService(
			artists, aliases, client, statuses);
		var response = service.syncPendingAliases(100);

		assertThat(response.candidateArtistCount()).isEqualTo(1);
		assertThat(response.resolvedAliasCount()).isEqualTo(1);
		assertThat(response.failedCount()).isZero();
		ArgumentCaptor<ArtistAlias> aliasCaptor = ArgumentCaptor.forClass(ArtistAlias.class);
		verify(aliases).save(aliasCaptor.capture());
		assertThat(aliasCaptor.getValue().getKopisArtistId()).isEqualTo(2L);
		assertThat(aliasCaptor.getValue().getSpotifyArtistId())
			.isEqualTo("24nUVBIlCGi4twz4nYxJum");
		assertThat(aliasCaptor.getValue().getResolutionStatus())
			.isEqualTo(ArtistAliasResolutionStatus.RESOLVED);
		verify(statuses).save(any());
	}
}
