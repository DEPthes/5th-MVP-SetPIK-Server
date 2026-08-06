package com.setpik.server.member.service;

import com.setpik.server.common.exception.BusinessException;
import com.setpik.server.common.exception.ErrorCode;
import com.setpik.server.member.domain.User;
import com.setpik.server.member.domain.UserStatus;
import com.setpik.server.member.dto.SpotifyAccountProfileResponse;
import com.setpik.server.member.dto.UserProfileResponse;
import com.setpik.server.member.repository.UserRepository;
import com.setpik.server.spotify.domain.ConnectionStatus;
import com.setpik.server.spotify.domain.SpotifyAccount;
import com.setpik.server.spotify.repository.SpotifyAccountRepository;
import java.time.ZoneId;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService {

	private static final ZoneId SERVICE_ZONE_ID = ZoneId.of("Asia/Seoul");
	private final UserRepository userRepository;
	private final SpotifyAccountRepository spotifyAccountRepository;

	public UserProfileService(
		UserRepository userRepository,
		SpotifyAccountRepository spotifyAccountRepository
	) {
		this.userRepository = userRepository;
		this.spotifyAccountRepository = spotifyAccountRepository;
	}

	/** 인증된 회원과 Spotify 연결 정보를 조회 전용 DTO로 조립한다. */
	@Transactional(readOnly = true)
	public UserProfileResponse getMyProfile(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		Optional<SpotifyAccount> connectedAccount = spotifyAccountRepository.findByUserId(userId)
			.filter(account -> account.getConnectionStatus() == ConnectionStatus.CONNECTED);

		return new UserProfileResponse(
			user.getUserId(),
			user.getStatus(),
			user.getLastLoginAt() == null
				? null
				: user.getLastLoginAt().atZone(SERVICE_ZONE_ID).toOffsetDateTime(),
			connectedAccount.isPresent(),
			connectedAccount.map(this::toSpotifyProfile).orElse(null)
		);
	}

	private SpotifyAccountProfileResponse toSpotifyProfile(SpotifyAccount account) {
		return new SpotifyAccountProfileResponse(
			account.getSpotifyUserId(),
			account.getDisplayName(),
			account.getProfileImageUrl()
		);
	}
}
