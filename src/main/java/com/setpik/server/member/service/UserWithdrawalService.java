package com.setpik.server.member.service;

import com.setpik.server.auth.repository.AuthRefreshTokenRepository;
import com.setpik.server.common.exception.BusinessException;
import com.setpik.server.common.exception.ErrorCode;
import com.setpik.server.favorite.repository.FavoritePerformanceRepository;
import com.setpik.server.member.domain.User;
import com.setpik.server.member.domain.UserStatus;
import com.setpik.server.member.repository.UserRepository;
import com.setpik.server.playlist.repository.SpotifyPlaylistRepository;
import com.setpik.server.spotify.repository.SpotifyAccountRepository;
import com.setpik.server.spotify.repository.SpotifyAccountScopeRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserWithdrawalService {

	private final UserRepository userRepository;
	private final AuthRefreshTokenRepository refreshTokenRepository;
	private final SpotifyAccountRepository spotifyAccountRepository;
	private final SpotifyAccountScopeRepository spotifyAccountScopeRepository;
	private final FavoritePerformanceRepository favoritePerformanceRepository;
	private final SpotifyPlaylistRepository spotifyPlaylistRepository;
	private final Clock clock;

	public UserWithdrawalService(
		UserRepository userRepository,
		AuthRefreshTokenRepository refreshTokenRepository,
		SpotifyAccountRepository spotifyAccountRepository,
		SpotifyAccountScopeRepository spotifyAccountScopeRepository,
		FavoritePerformanceRepository favoritePerformanceRepository,
		SpotifyPlaylistRepository spotifyPlaylistRepository,
		Clock clock
	) {
		this.userRepository = userRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.spotifyAccountRepository = spotifyAccountRepository;
		this.spotifyAccountScopeRepository = spotifyAccountScopeRepository;
		this.favoritePerformanceRepository = favoritePerformanceRepository;
		this.spotifyPlaylistRepository = spotifyPlaylistRepository;
		this.clock = clock;
	}

	/** 회원과 인증 정보를 비활성화하고 soft delete를 지원하는 소유 리소스를 숨긴다. */
	@Transactional
	public void withdraw(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
		if (user.getStatus() == UserStatus.WITHDRAWN) {
			throw new BusinessException(ErrorCode.DUPLICATE_REQUEST);
		}

		LocalDateTime now = LocalDateTime.now(clock);
		user.withdraw();
		refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(userId)
			.forEach(token -> token.revoke(now));
		spotifyAccountRepository.findByUserId(userId).ifPresent(account -> {
			account.disconnect(now);
			spotifyAccountScopeRepository
				.findAllBySpotifyAccountIdOrderByScopeNameAsc(account.getSpotifyAccountId())
				.forEach(scope -> scope.revoke(now));
		});
		favoritePerformanceRepository.findAllByUserIdAndDeletedAtIsNull(userId)
			.forEach(favorite -> favorite.delete(now));
		spotifyPlaylistRepository.findAllByUserIdAndDeletedAtIsNull(userId)
			.forEach(playlist -> playlist.delete(now));
	}
}
