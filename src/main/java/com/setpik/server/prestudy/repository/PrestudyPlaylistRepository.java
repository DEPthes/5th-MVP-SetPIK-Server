package com.setpik.server.prestudy.repository;

import com.setpik.server.prestudy.domain.PrestudyPlaylist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** 기본 CRUD는 JpaRepository가 제공하고 도메인별 조회 메서드는 여기에 추가한다. */
public interface PrestudyPlaylistRepository extends JpaRepository<PrestudyPlaylist, Long> {

	Page<PrestudyPlaylist> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

	Optional<PrestudyPlaylist> findByPrestudyPlaylistIdAndUserId(Long prestudyPlaylistId, Long userId);
}