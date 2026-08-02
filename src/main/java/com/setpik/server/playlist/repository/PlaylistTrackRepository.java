package com.setpik.server.playlist.repository;

import com.setpik.server.playlist.domain.PlaylistTrack;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** 기본 CRUD는 JpaRepository가 제공하고 도메인별 조회 메서드는 여기에 추가한다. */
public interface PlaylistTrackRepository extends JpaRepository<PlaylistTrack, Long> {
	List<PlaylistTrack> findByPlaylistIdOrderByTrackPositionAsc(Long playlistId);
}
