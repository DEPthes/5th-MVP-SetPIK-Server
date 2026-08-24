package com.setpik.server.playlist.repository;

import com.setpik.server.playlist.domain.PlaylistRecentSelection;
import com.setpik.server.playlist.domain.PlaylistRecentSelectionId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** 기본 CRUD는 JpaRepository가 제공하고 도메인별 조회 메서드는 여기에 추가한다. */
public interface PlaylistRecentSelectionRepository extends JpaRepository<PlaylistRecentSelection, PlaylistRecentSelectionId> {

	Page<PlaylistRecentSelection> findByUserId(Long userId, Pageable pageable);

	List<PlaylistRecentSelection> findByUserIdOrderBySelectedAtDescPlaylistIdDesc(Long userId);

	Optional<PlaylistRecentSelection> findFirstByUserIdOrderBySelectedAtDescPlaylistIdDesc(Long userId);
}
