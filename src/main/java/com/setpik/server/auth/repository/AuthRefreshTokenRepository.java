package com.setpik.server.auth.repository;

import com.setpik.server.auth.domain.AuthRefreshToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 기본 CRUD는 JpaRepository가 제공하고 도메인별 조회 메서드는 여기에 추가한다. */
public interface AuthRefreshTokenRepository extends JpaRepository<AuthRefreshToken, Long> {

	Optional<AuthRefreshToken> findByTokenHash(String tokenHash);

	List<AuthRefreshToken> findAllByUserIdAndRevokedAtIsNull(Long userId);
}
