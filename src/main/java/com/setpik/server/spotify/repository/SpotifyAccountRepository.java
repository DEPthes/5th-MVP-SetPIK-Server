package com.setpik.server.spotify.repository;

import com.setpik.server.spotify.domain.SpotifyAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 기본 CRUD는 JpaRepository가 제공하고 도메인별 조회 메서드는 여기에 추가한다. */
public interface SpotifyAccountRepository extends JpaRepository<SpotifyAccount, Long> {

	Optional<SpotifyAccount> findBySpotifyUserId(String spotifyUserId);
}
