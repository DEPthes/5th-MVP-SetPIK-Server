package com.setpik.server.auth.service;

import com.setpik.server.auth.client.dto.SpotifyProfileResponse;
import com.setpik.server.auth.client.dto.SpotifyTokenResponse;
import com.setpik.server.auth.config.SetpikAuthProperties;
import com.setpik.server.auth.domain.AuthRefreshToken;
import com.setpik.server.auth.dto.SpotifyCallbackResult;
import com.setpik.server.auth.repository.AuthRefreshTokenRepository;
import com.setpik.server.auth.security.RefreshTokenGenerator;
import com.setpik.server.auth.security.TokenCipher;
import com.setpik.server.auth.security.TokenHasher;
import com.setpik.server.member.domain.User;
import com.setpik.server.member.repository.UserRepository;
import com.setpik.server.spotify.domain.SpotifyAccount;
import com.setpik.server.spotify.domain.SpotifyAccountScope;
import com.setpik.server.spotify.repository.SpotifyAccountRepository;
import com.setpik.server.spotify.repository.SpotifyAccountScopeRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SpotifyLoginPersistenceService {

	private final UserRepository userRepository;
	private final SpotifyAccountRepository spotifyAccountRepository;
	private final SpotifyAccountScopeRepository spotifyAccountScopeRepository;
	private final AuthRefreshTokenRepository authRefreshTokenRepository;
	private final TokenCipher tokenCipher;
	private final RefreshTokenGenerator refreshTokenGenerator;
	private final TokenHasher tokenHasher;
	private final SetpikAuthProperties authProperties;
	private final Clock clock;

	public SpotifyLoginPersistenceService(
		UserRepository userRepository,
		SpotifyAccountRepository spotifyAccountRepository,
		SpotifyAccountScopeRepository spotifyAccountScopeRepository,
		AuthRefreshTokenRepository authRefreshTokenRepository,
		TokenCipher tokenCipher,
		RefreshTokenGenerator refreshTokenGenerator,
		TokenHasher tokenHasher,
		SetpikAuthProperties authProperties,
		Clock clock
	) {
		this.userRepository = userRepository;
		this.spotifyAccountRepository = spotifyAccountRepository;
		this.spotifyAccountScopeRepository = spotifyAccountScopeRepository;
		this.authRefreshTokenRepository = authRefreshTokenRepository;
		this.tokenCipher = tokenCipher;
		this.refreshTokenGenerator = refreshTokenGenerator;
		this.tokenHasher = tokenHasher;
		this.authProperties = authProperties;
		this.clock = clock;
	}

	/** 회원·Spotify 계정·scope·SetPIK Refresh Token을 하나의 트랜잭션으로 저장한다. */
	@Transactional
	public SpotifyCallbackResult saveLogin(
		SpotifyTokenResponse tokenResponse,
		SpotifyProfileResponse profileResponse
	) {
		LocalDateTime now = LocalDateTime.now(clock);
		String encryptedAccessToken = tokenCipher.encrypt(tokenResponse.accessToken());
		String encryptedRefreshToken = tokenCipher.encrypt(tokenResponse.refreshToken());
		LocalDateTime tokenExpiresAt = now.plusSeconds(tokenResponse.expiresIn());

		SpotifyAccount spotifyAccount = spotifyAccountRepository
			.findBySpotifyUserId(profileResponse.accountIdentifier())
			.map(account -> updateExistingAccount(
				account,
				profileResponse,
				encryptedAccessToken,
				encryptedRefreshToken,
				tokenExpiresAt,
				now
			))
			.orElseGet(() -> createAccount(
				profileResponse,
				encryptedAccessToken,
				encryptedRefreshToken,
				tokenExpiresAt,
				now
			));

		replaceGrantedScopes(spotifyAccount, tokenResponse.scope(), now);
		String refreshToken = issueSetpikRefreshToken(spotifyAccount.getUserId(), now);
		return new SpotifyCallbackResult(refreshToken);
	}

	private SpotifyAccount createAccount(
		SpotifyProfileResponse profile,
		String encryptedAccessToken,
		String encryptedRefreshToken,
		LocalDateTime tokenExpiresAt,
		LocalDateTime now
	) {
		User user = userRepository.saveAndFlush(User.createActive(now));
		SpotifyAccount account = SpotifyAccount.connect(
			profile.accountIdentifier(),
			profile.email(),
			profile.displayName(),
			profile.firstImageUrl(),
			encryptedAccessToken,
			encryptedRefreshToken,
			tokenExpiresAt,
			user.getUserId(),
			now
		);
		return spotifyAccountRepository.saveAndFlush(account);
	}

	private SpotifyAccount updateExistingAccount(
		SpotifyAccount account,
		SpotifyProfileResponse profile,
		String encryptedAccessToken,
		String encryptedRefreshToken,
		LocalDateTime tokenExpiresAt,
		LocalDateTime now
	) {
		User user = userRepository.findById(account.getUserId())
			.orElseThrow(() -> new IllegalStateException("Spotify 계정에 연결된 회원을 찾을 수 없습니다."));
		user.recordLogin(now);
		account.reconnect(
			profile.email(),
			profile.displayName(),
			profile.firstImageUrl(),
			encryptedAccessToken,
			encryptedRefreshToken,
			tokenExpiresAt,
			now
		);
		return account;
	}

	private void replaceGrantedScopes(
		SpotifyAccount account,
		String scopes,
		LocalDateTime grantedAt
	) {
		spotifyAccountScopeRepository.deleteAllBySpotifyAccountId(account.getSpotifyAccountId());
		List<SpotifyAccountScope> grantedScopes = parseScopes(scopes).stream()
			.map(scope -> SpotifyAccountScope.grant(scope, account.getSpotifyAccountId(), grantedAt))
			.toList();
		spotifyAccountScopeRepository.saveAll(grantedScopes);
	}

	private List<String> parseScopes(String scopes) {
		if (scopes == null || scopes.isBlank()) {
			return List.of();
		}
		return Arrays.stream(scopes.trim().split("\\s+"))
			.distinct()
			.toList();
	}

	private String issueSetpikRefreshToken(Long userId, LocalDateTime issuedAt) {
		String refreshToken = refreshTokenGenerator.generate();
		AuthRefreshToken tokenEntity = AuthRefreshToken.issue(
			tokenHasher.hash(refreshToken),
			issuedAt.plus(authProperties.refreshTokenExpiration()),
			userId
		);
		authRefreshTokenRepository.save(tokenEntity);
		return refreshToken;
	}
}
