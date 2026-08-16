package com.setpik.server.artist.repository;

import com.setpik.server.artist.domain.Artist;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 기본 CRUD는 JpaRepository가 제공하고 도메인별 조회 메서드는 여기에 추가한다. */
public interface ArtistRepository extends JpaRepository<Artist, Long> {
	Optional<Artist> findBySpotifyArtistId(String spotifyArtistId);
	Optional<Artist> findByNormalizedName(String normalizedName);
	List<Artist> findByNormalizedNameIn(List<String> normalizedNames);
}
