package com.setpik.server.artist.repository;

import com.setpik.server.artist.domain.Artist;
import com.setpik.server.artist.domain.Genre;
import com.setpik.server.performance.domain.Performance;
import com.setpik.server.performance.domain.PerformanceArtist;
import com.setpik.server.performance.domain.PerformanceGenre;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;

/** 기본 CRUD는 JpaRepository가 제공하고 도메인별 조회 메서드는 여기에 추가한다. */
public interface ArtistRepository extends JpaRepository<Artist, Long> {
	Optional<Artist> findBySpotifyArtistId(String spotifyArtistId);
	Optional<Artist> findByNormalizedName(String normalizedName);
	List<Artist> findByNormalizedNameIn(List<String> normalizedNames);

	/** 기존 동기화 과정에서 Spotify와 연결되지 않은 음악 공연 출연진만 조회한다. */
	@Query("""
		select distinct artist
		from Artist artist
		join PerformanceArtist performanceArtist on performanceArtist.artistId = artist.artistId
		join Performance performance on performance.performanceId = performanceArtist.performanceId
		join PerformanceGenre performanceGenre on performanceGenre.performanceId = performance.performanceId
		join Genre genre on genre.genreId = performanceGenre.genreId
		where artist.spotifyArtistId is null
		  and artist.spotifyAvailable = false
		  and performance.isDeleted = false
		  and artist.artistId > :afterArtistId
		  and genre.genreName like concat('%', '음악', '%')
		order by artist.artistId asc
		""")
	List<Artist> findKopisOnlyArtistsInMusicPerformancesAfterId(
		Long afterArtistId,
		Pageable pageable
	);
}
