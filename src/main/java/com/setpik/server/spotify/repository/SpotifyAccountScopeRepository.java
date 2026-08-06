package com.setpik.server.spotify.repository;

import com.setpik.server.spotify.domain.SpotifyAccountScope;
import com.setpik.server.spotify.domain.SpotifyAccountScopeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 기본 CRUD는 JpaRepository가 제공하고 도메인별 조회 메서드는 여기에 추가한다. */
public interface SpotifyAccountScopeRepository extends JpaRepository<SpotifyAccountScope, SpotifyAccountScopeId> {

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("delete from SpotifyAccountScope scope where scope.spotifyAccountId = :spotifyAccountId")
	void deleteAllBySpotifyAccountId(@Param("spotifyAccountId") Long spotifyAccountId);
}
