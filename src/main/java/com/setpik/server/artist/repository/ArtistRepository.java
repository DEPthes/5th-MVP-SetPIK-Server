package com.setpik.server.artist.repository;

import com.setpik.server.artist.domain.Artist;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

/** 기본 CRUD는 JpaRepository가 제공하고 도메인별 조회 메서드는 여기에 추가한다. */
public interface ArtistRepository extends JpaRepository<Artist, Long> {
	Optional<Artist> findBySpotifyArtistId(String spotifyArtistId);
	Optional<Artist> findByNormalizedName(String normalizedName);
	List<Artist> findByNormalizedNameIn(List<String> normalizedNames);

	@Query(value = """
		select distinct a.artist_id
		from Artists a
		join Performance_Artists pa on pa.artist_id = a.artist_id
		join Performance_Genres pg on pg.performance_id = pa.performance_id
		join Genres g on g.genre_id = pg.genre_id
		left join Artist_Aliases alias_mapping on alias_mapping.kopis_artist_id = a.artist_id
		where a.spotify_available = false
		  and (alias_mapping.kopis_artist_id is null or alias_mapping.resolution_status = 'FAILED')
		  and g.genre_name like concat('%', :genreKeyword, '%')
		order by a.artist_id asc
		limit :limit
		""", nativeQuery = true)
	List<Long> findUnresolvedKopisMusicArtistIds(
		@Param("genreKeyword") String genreKeyword,
		@Param("limit") int limit
	);

	@Query(value = """
		select a.artist_id
		from Artists a
		left join Artist_Aliases alias_mapping on alias_mapping.kopis_artist_id = a.artist_id
		left join Artist_Genre_Sync_Status sync_status on sync_status.artist_id = a.artist_id
		where (
		  a.spotify_artist_id is not null
		  or (alias_mapping.resolution_status = 'RESOLVED' and alias_mapping.spotify_artist_id is not null)
		)
		and not exists (
		  select 1 from Artists_Genres ag where ag.artist_id = a.artist_id
		)
		and (sync_status.artist_id is null or sync_status.resolution_status = 'FAILED')
		order by case when exists (
		  select 1 from Analysis_Artists aa where aa.artist_id = a.artist_id and aa.is_major = true
		) then 0 else 1 end, a.artist_id asc
		limit :limit
		""", nativeQuery = true)
	List<Long> findPendingGenreSyncArtistIds(@Param("limit") int limit);
}
