package com.setpik.server.artist.repository;

import com.setpik.server.artist.domain.Genre;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

/** 기본 CRUD는 JpaRepository가 제공하고 도메인별 조회 메서드는 여기에 추가한다. */
public interface GenreRepository extends JpaRepository<Genre, Long> {
	Optional<Genre> findByNormalizedName(String normalizedName);
	List<Genre> findByNormalizedNameIn(List<String> normalizedNames);
}
