package com.setpik.server.artist.repository;

import com.setpik.server.artist.domain.SpotifyArtistNameAlias;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpotifyArtistNameAliasRepository extends JpaRepository<SpotifyArtistNameAlias, Long> {
	List<SpotifyArtistNameAlias> findByArtistIdIn(Collection<Long> artistIds);
	void deleteByArtistId(Long artistId);
}
