package com.setpik.server.member.service;

import com.setpik.server.common.exception.BusinessException;
import com.setpik.server.common.exception.ErrorCode;
import com.setpik.server.common.storage.ImageStorageClient;
import com.setpik.server.member.domain.User;
import com.setpik.server.member.domain.UserStatus;
import com.setpik.server.member.dto.ProfileImageResponse;
import com.setpik.server.member.dto.SpotifyAccountProfileResponse;
import com.setpik.server.member.dto.UpdateUserProfileRequest;
import com.setpik.server.member.dto.UserProfileResponse;
import com.setpik.server.member.repository.UserRepository;
import com.setpik.server.spotify.domain.ConnectionStatus;
import com.setpik.server.spotify.domain.SpotifyAccount;
import com.setpik.server.spotify.repository.SpotifyAccountRepository;
import java.time.ZoneId;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserProfileService {

	private static final ZoneId SERVICE_ZONE_ID = ZoneId.of("Asia/Seoul");
	private final UserRepository userRepository;
	private final SpotifyAccountRepository spotifyAccountRepository;
	private final ImageStorageClient imageStorageClient;

	public UserProfileService(
		UserRepository userRepository,
		SpotifyAccountRepository spotifyAccountRepository,
		ImageStorageClient imageStorageClient
	) {
		this.userRepository = userRepository;
		this.spotifyAccountRepository = spotifyAccountRepository;
		this.imageStorageClient = imageStorageClient;
	}

	/** 인증된 회원과 Spotify 연결 정보를 조회 전용 DTO로 조립한다. */
	@Transactional(readOnly = true)
	public UserProfileResponse getMyProfile(Long userId) {
		User user = getActiveUser(userId);
		return toProfileResponse(user);
	}

	/** nickname, birthDate를 각각 전달된 값만 개별적으로 수정한다. */
	@Transactional
	public UserProfileResponse updateMyProfile(Long userId, UpdateUserProfileRequest request) {
		User user = getActiveUser(userId);

		if (request.nickname() != null) {
			String trimmedNickname = request.nickname().trim();
			if (trimmedNickname.isEmpty()) {
				throw new BusinessException(ErrorCode.INVALID_REQUEST);
			}
			user.updateNickname(trimmedNickname);
		}
		if (request.birthDate() != null) {
			user.updateBirthDate(request.birthDate());
		}

		return toProfileResponse(user);
	}

	/** 이미지를 업로드하고 프로필 이미지 URL을 교체한다. */
	@Transactional
	public ProfileImageResponse updateProfileImage(Long userId, MultipartFile image) {
		if (image == null || image.isEmpty()) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}
		String contentType = image.getContentType();
		if (contentType == null || !contentType.startsWith("image/")) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}

		User user = getActiveUser(userId);
		String uploadedUrl = imageStorageClient.upload(image);
		user.updateProfileImageUrl(uploadedUrl);
		return new ProfileImageResponse(user.getProfileImageUrl());
	}

	/** 프로필 이미지를 기본 이미지로 초기화한다. */
	@Transactional
	public ProfileImageResponse resetProfileImage(Long userId) {
		User user = getActiveUser(userId);
		user.resetProfileImageUrl();
		return new ProfileImageResponse(user.getProfileImageUrl());
	}

	private User getActiveUser(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
		return user;
	}

	private UserProfileResponse toProfileResponse(User user) {
		Optional<SpotifyAccount> connectedAccount = spotifyAccountRepository.findByUserId(user.getUserId())
			.filter(account -> account.getConnectionStatus() == ConnectionStatus.CONNECTED);

		return new UserProfileResponse(
			user.getUserId(),
			user.getStatus(),
			user.getLastLoginAt() == null
				? null
				: user.getLastLoginAt().atZone(SERVICE_ZONE_ID).toOffsetDateTime(),
			user.getNickname(),
			user.getBirthDate(),
			user.getProfileImageUrl(),
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
