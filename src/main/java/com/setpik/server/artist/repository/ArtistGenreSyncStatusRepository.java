package com.setpik.server.artist.repository;

import com.setpik.server.artist.domain.ArtistGenreSyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtistGenreSyncStatusRepository extends JpaRepository<ArtistGenreSyncStatus, Long> {
}
