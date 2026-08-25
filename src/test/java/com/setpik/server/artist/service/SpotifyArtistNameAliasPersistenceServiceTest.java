package com.setpik.server.artist.service;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import com.setpik.server.artist.domain.SpotifyArtistNameAlias;
import com.setpik.server.artist.repository.SpotifyArtistNameAliasRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class SpotifyArtistNameAliasPersistenceServiceTest {

	@Test
	void replacesExistingAliasesInsideOnePersistenceOperation() {
		SpotifyArtistNameAliasRepository repository = mock(SpotifyArtistNameAliasRepository.class);
		SpotifyArtistNameAliasPersistenceService service =
			new SpotifyArtistNameAliasPersistenceService(repository);
		List<SpotifyArtistNameAlias> aliases = List.of(
			SpotifyArtistNameAlias.wikidata(1L, "프로미스나인", "프로미스나인", "Q41398331"));

		service.replaceAliases(1L, aliases);

		InOrder ordered = inOrder(repository);
		ordered.verify(repository).deleteByArtistId(1L);
		ordered.verify(repository).saveAll(aliases);
	}
}
