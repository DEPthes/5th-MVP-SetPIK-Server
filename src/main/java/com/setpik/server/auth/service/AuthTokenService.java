package com.setpik.server.auth.service;

import com.setpik.server.auth.domain.AuthRefreshToken;
import com.setpik.server.auth.dto.AccessTokenResponse;
import com.setpik.server.auth.repository.AuthRefreshTokenRepository;
import com.setpik.server.auth.security.JwtAccessTokenProvider;
import com.setpik.server.auth.security.TokenHasher;
import com.setpik.server.common.exception.BusinessException;
import com.setpik.server.common.exception.ErrorCode;
import com.setpik.server.member.domain.User;
import com.setpik.server.member.domain.UserStatus;
import com.setpik.server.member.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthTokenService {

	private final AuthRefreshTokenRepository refreshTokenRepository;
	private final UserRepository userRepository;
	private final TokenHasher tokenHasher;
	private final JwtAccessTokenProvider accessTokenProvider;
	private final Clock clock;

	public AuthTokenService(
		AuthRefreshTokenRepository refreshTokenRepository,
		UserRepository userRepository,
		TokenHasher tokenHasher,
		JwtAccessTokenProvider accessTokenProvider,
		Clock clock
	) {
		this.refreshTokenRepository = refreshTokenRepository;
		this.userRepository = userRepository;
		this.tokenHasher = tokenHasher;
		this.accessTokenProvider = accessTokenProvider;
		this.clock = clock;
	}

	/** 쿠키 원문을 해시로 조회해 검증하고 새 Access Token을 발급한다. */
	@Transactional
	public AccessTokenResponse reissueAccessToken(String rawRefreshToken) {
		if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		AuthRefreshToken refreshToken = refreshTokenRepository
			.findByTokenHash(tokenHasher.hash(rawRefreshToken))
			.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
		LocalDateTime now = LocalDateTime.now(clock);
		if (!refreshToken.isUsableAt(now)) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		User user = userRepository.findById(refreshToken.getUserId())
			.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		refreshToken.markUsed(now);
		return new AccessTokenResponse(accessTokenProvider.issue(user.getUserId()));
	}
}
