package com.setpik.server.performance.repository;

import com.setpik.server.performance.domain.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

/** 기본 CRUD는 JpaRepository가 제공하고 도메인별 조회 메서드는 여기에 추가한다. */
public interface VenueRepository extends JpaRepository<Venue, Long> {
	Optional<Venue> findByKopisVenueId(String kopisVenueId);
	List<Venue> findByKopisVenueIdIn(List<String> kopisVenueIds);
}
