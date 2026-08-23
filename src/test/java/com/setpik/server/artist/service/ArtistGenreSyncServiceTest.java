package com.setpik.server.artist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.setpik.server.artist.client.WikidataArtistGenreClient;
import com.setpik.server.artist.domain.Artist;
import com.setpik.server.artist.domain.ArtistGenre;
import com.setpik.server.artist.domain.Genre;
import com.setpik.server.artist.repository.ArtistAliasRepository;
import com.setpik.server.artist.repository.ArtistGenreRepository;
import com.setpik.server.artist.repository.ArtistGenreSyncStatusRepository;
import com.setpik.server.artist.repository.ArtistRepository;
import com.setpik.server.artist.repository.GenreRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ArtistGenreSyncServiceTest {
	@Test
	void storesDetailedWikidataGenresForSpotifyArtist() {
		ArtistRepository artists = mock(ArtistRepository.class);
		ArtistAliasRepository aliases = mock(ArtistAliasRepository.class);
		GenreRepository genres = mock(GenreRepository.class);
		ArtistGenreRepository artistGenres = mock(ArtistGenreRepository.class);
		ArtistGenreSyncStatusRepository statuses = mock(ArtistGenreSyncStatusRepository.class);
		WikidataArtistGenreClient client = mock(WikidataArtistGenreClient.class);
		Artist artist = mock(Artist.class);
		when(artist.getArtistId()).thenReturn(7L);
		when(artist.getSpotifyArtistId()).thenReturn("spotify-7");
		when(artists.findPendingGenreSyncArtistIds(20)).thenReturn(List.of(7L));
		when(artists.findAllById(List.of(7L))).thenReturn(List.of(artist));
		when(aliases.findByKopisArtistIdIn(List.of(7L))).thenReturn(List.of());
		when(client.lookup("spotify-7")).thenReturn(WikidataArtistGenreClient.LookupResult
			.resolved("Q7", Set.of("K-pop", "Korean hip hop")));
		when(genres.findByNormalizedName(any())).thenReturn(Optional.empty());
		when(genres.save(any(Genre.class))).thenAnswer(invocation -> {
			Genre genre = mock(Genre.class);
			when(genre.getGenreId()).thenReturn(invocation.getArgument(0, Genre.class)
				.getGenreName().equals("K_POP") ? 10L : 11L);
			return genre;
		});

		var service = new ArtistGenreSyncService(artists, aliases, genres, artistGenres, statuses, client);
		var response = service.syncPendingGenres(20);

		assertThat(response.resolvedArtistCount()).isEqualTo(1);
		assertThat(response.savedGenreCount()).isEqualTo(2);
		ArgumentCaptor<ArtistGenre> captor = ArgumentCaptor.forClass(ArtistGenre.class);
		verify(artistGenres, org.mockito.Mockito.times(2)).save(captor.capture());
		assertThat(captor.getAllValues()).extracting(ArtistGenre::getSourceType)
			.containsOnly("WIKIDATA");
		verify(statuses).save(any());
	}
}
