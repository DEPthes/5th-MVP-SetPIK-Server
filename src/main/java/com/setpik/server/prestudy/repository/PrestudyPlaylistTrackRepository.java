package com.setpik.server.prestudy.repository;

import com.setpik.server.prestudy.domain.PrestudyPlaylistTrack;
import com.setpik.server.prestudy.domain.PrestudyPlaylistTrackId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** 기본 CRUD는 JpaRepository가 제공하고 도메인별 조회 메서드는 여기에 추가한다. */
public interface PrestudyPlaylistTrackRepository extends JpaRepository<PrestudyPlaylistTrack, PrestudyPlaylistTrackId> {

	List<PrestudyPlaylistTrack> findByPrestudyPlaylistIdOrderByTrackOrderAsc(Long prestudyPlaylistId);
}