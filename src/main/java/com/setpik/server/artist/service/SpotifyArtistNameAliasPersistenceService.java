package com.setpik.server.artist.service;

import com.setpik.server.artist.domain.SpotifyArtistNameAlias;
import com.setpik.server.artist.repository.SpotifyArtistNameAliasRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Wikidata 이름 별칭 교체 작업의 짧은 DB 트랜잭션 경계를 담당한다. */
@Service
public class SpotifyArtistNameAliasPersistenceService {

	private final SpotifyArtistNameAliasRepository nameAliasRepository;

	public SpotifyArtistNameAliasPersistenceService(
		SpotifyArtistNameAliasRepository nameAliasRepository
	) {
		this.nameAliasRepository = nameAliasRepository;
	}

	@Transactional
	public void replaceAliases(Long artistId, List<SpotifyArtistNameAlias> aliases) {
		nameAliasRepository.deleteByArtistId(artistId);
		nameAliasRepository.saveAll(aliases);
	}
}
