package com.setpik.server.spotify.service;

import com.setpik.server.common.exception.BusinessException;
import com.setpik.server.common.exception.ErrorCode;
import com.setpik.server.member.domain.User;
import com.setpik.server.member.domain.UserStatus;
import com.setpik.server.member.repository.UserRepository;
import com.setpik.server.spotify.domain.ConnectionStatus;
import com.setpik.server.spotify.domain.SpotifyAccount;
import com.setpik.server.spotify.dto.SpotifyConnectionResponse;
import com.setpik.server.spotify.dto.SpotifyScopeResponse;
import com.setpik.server.spotify.repository.SpotifyAccountRepository;
import com.setpik.server.spotify.repository.SpotifyAccountScopeRepository;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SpotifyConnectionService {

	private static final ZoneId SERVICE_ZONE_ID = ZoneId.of("Asia/Seoul");
	private final UserRepository userRepository;
	private final SpotifyAccountRepository spotifyAccountRepository;
	private final SpotifyAccountScopeRepository spotifyAccountScopeRepository;

	public SpotifyConnectionService(
		UserRepository userRepository,
		SpotifyAccountRepository spotifyAccountRepository,
		SpotifyAccountScopeRepository spotifyAccountScopeRepository
	) {
		this.userRepository = userRepository;
		this.spotifyAccountRepository = spotifyAccountRepository;
		this.spotifyAccountScopeRepository = spotifyAccountScopeRepository;
	}

	/** 인증된 회원의 Spotify 연결 상태와 현재 저장된 scope를 조회한다. */
	@Transactional(readOnly = true)
	public SpotifyConnectionResponse getConnection(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		return spotifyAccountRepository.findByUserId(userId)
			.map(this::toConnectionResponse)
			.orElseGet(this::disconnectedResponse);
	}

	private SpotifyConnectionResponse toConnectionResponse(SpotifyAccount account) {
		List<SpotifyScopeResponse> scopes = spotifyAccountScopeRepository
			.findAllBySpotifyAccountIdOrderByScopeNameAsc(account.getSpotifyAccountId())
			.stream()
			.map(scope -> new SpotifyScopeResponse(scope.getScopeName(), scope.getIsGranted()))
			.toList();

		return new SpotifyConnectionResponse(
			account.getConnectionStatus() == ConnectionStatus.CONNECTED,
			account.getConnectionStatus(),
			account.getTokenExpiresAt() == null
				? null
				: account.getTokenExpiresAt().atZone(SERVICE_ZONE_ID).toOffsetDateTime(),
			scopes
		);
	}

	private SpotifyConnectionResponse disconnectedResponse() {
		return new SpotifyConnectionResponse(
			false,
			ConnectionStatus.DISCONNECTED,
			null,
			List.of()
		);
	}
}
