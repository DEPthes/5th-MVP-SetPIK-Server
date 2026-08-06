package com.setpik.server.auth.service;

import com.setpik.server.auth.domain.AuthRefreshToken;
import com.setpik.server.auth.repository.AuthRefreshTokenRepository;
import com.setpik.server.auth.security.TokenHasher;
import com.setpik.server.common.exception.BusinessException;
import com.setpik.server.common.exception.ErrorCode;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthLogoutService {

	private final AuthRefreshTokenRepository refreshTokenRepository;
	private final TokenHasher tokenHasher;
	private final Clock clock;

	public AuthLogoutService(
		AuthRefreshTokenRepository refreshTokenRepository,
		TokenHasher tokenHasher,
		Clock clock
	) {
		this.refreshTokenRepository = refreshTokenRepository;
		this.tokenHasher = tokenHasher;
		this.clock = clock;
	}

	/** 쿠키 원문을 해시로 조회하고 유효한 Refresh Token의 revoked_at을 기록한다. */
	@Transactional
	public void logout(String rawRefreshToken) {
		if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}

		AuthRefreshToken refreshToken = refreshTokenRepository
			.findByTokenHash(tokenHasher.hash(rawRefreshToken))
			.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST));
		LocalDateTime now = LocalDateTime.now(clock);
		if (!refreshToken.isUsableAt(now)) {
			throw new BusinessException(ErrorCode.DUPLICATE_REQUEST);
		}

		refreshToken.revoke(now);
	}
}
