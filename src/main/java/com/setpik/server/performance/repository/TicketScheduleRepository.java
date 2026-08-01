package com.setpik.server.performance.repository;

import com.setpik.server.performance.domain.TicketSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

/** 기본 CRUD는 JpaRepository가 제공하고 도메인별 조회 메서드는 여기에 추가한다. */
public interface TicketScheduleRepository extends JpaRepository<TicketSchedule, Long> {
}
