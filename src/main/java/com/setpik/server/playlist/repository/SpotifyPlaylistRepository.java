package com.setpik.server.playlist.repository;

import com.setpik.server.playlist.domain.SpotifyPlaylist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** 기본 CRUD는 JpaRepository가 제공하고 도메인별 조회 메서드는 여기에 추가한다. */
public interface SpotifyPlaylistRepository extends JpaRepository<SpotifyPlaylist, Long> {
	List<SpotifyPlaylist> findByUserIdAndDeletedAtIsNull(Long userId);
	Optional<SpotifyPlaylist> findByPlaylistIdAndDeletedAtIsNull(Long playlistId);
	Optional<SpotifyPlaylist> findByUserIdAndSpotifyPlaylistId(Long userId, String spotifyPlaylistId);
}
