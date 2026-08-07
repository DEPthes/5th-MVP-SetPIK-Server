package com.setpik.server.playlist.repository;

import com.setpik.server.playlist.domain.SpotifyPlaylist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** 기본 CRUD는 JpaRepository가 제공하고 도메인별 조회 메서드는 여기에 추가한다. */
public interface SpotifyPlaylistRepository extends JpaRepository<SpotifyPlaylist, Long> {
	Page<SpotifyPlaylist> findByUserIdAndDeletedAtIsNullAndPlaylistNameContainingIgnoreCase(
		Long userId,
		String keyword,
		Pageable pageable
	);
	Optional<SpotifyPlaylist> findByPlaylistIdAndUserIdAndDeletedAtIsNull(
		Long playlistId,
		Long userId
	);
	Optional<SpotifyPlaylist> findByUserIdAndSpotifyPlaylistId(Long userId, String spotifyPlaylistId);
}
