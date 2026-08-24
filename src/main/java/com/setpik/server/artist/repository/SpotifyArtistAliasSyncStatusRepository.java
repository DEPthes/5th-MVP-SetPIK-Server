package com.setpik.server.artist.repository;

import com.setpik.server.artist.domain.SpotifyArtistAliasSyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpotifyArtistAliasSyncStatusRepository
	extends JpaRepository<SpotifyArtistAliasSyncStatus, Long> {
}
