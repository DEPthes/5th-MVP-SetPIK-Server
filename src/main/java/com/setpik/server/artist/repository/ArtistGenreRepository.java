package com.setpik.server.artist.repository;

import com.setpik.server.artist.domain.ArtistGenre;
import com.setpik.server.artist.domain.ArtistGenreId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 기본 CRUD는 JpaRepository가 제공하고 도메인별 조회 메서드는 여기에 추가한다. */
public interface ArtistGenreRepository extends JpaRepository<ArtistGenre, ArtistGenreId> {

	List<ArtistGenre> findByArtistIdIn(List<Long> artistIds);
}
