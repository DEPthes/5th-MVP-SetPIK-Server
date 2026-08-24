package com.setpik.server.artist.repository;

import com.setpik.server.artist.domain.ArtistAlias;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtistAliasRepository extends JpaRepository<ArtistAlias, Long> {
	List<ArtistAlias> findByKopisArtistIdIn(Collection<Long> kopisArtistIds);
	List<ArtistAlias> findBySpotifyArtistId(String spotifyArtistId);
}
