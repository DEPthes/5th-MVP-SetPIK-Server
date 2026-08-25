package com.setpik.server.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.setpik.server.common.exception.BusinessException;
import com.setpik.server.common.exception.ErrorCode;
import com.setpik.server.member.domain.User;
import com.setpik.server.member.dto.UpdateUserProfileRequest;
import com.setpik.server.member.dto.UserProfileResponse;
import com.setpik.server.member.repository.UserRepository;
import com.setpik.server.spotify.repository.SpotifyAccountRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserProfileServiceTest {

	private UserRepository userRepository;
	private SpotifyAccountRepository spotifyAccountRepository;
	private UserProfileService userProfileService;

	@BeforeEach
	void setUp() {
		userRepository = mock(UserRepository.class);
		spotifyAccountRepository = mock(SpotifyAccountRepository.class);
		userProfileService = new UserProfileService(userRepository, spotifyAccountRepository);
	}

	@Test
	void updatesOnlyNicknameWhenBirthDateIsOmitted() {
		User user = User.createActive(LocalDateTime.now());
		user.updateBirthDate(LocalDate.of(2000, 1, 1));
		when(userRepository.findById(any())).thenReturn(Optional.of(user));
		when(spotifyAccountRepository.findByUserId(any())).thenReturn(Optional.empty());

		UserProfileResponse response = userProfileService.updateMyProfile(
			1L, new UpdateUserProfileRequest("newNickname", null));

		assertThat(response.nickname()).isEqualTo("newNickname");
		assertThat(response.birthDate()).isEqualTo(LocalDate.of(2000, 1, 1));
	}

	@Test
	void updatesOnlyBirthDateWhenNicknameIsOmitted() {
		User user = User.createActive(LocalDateTime.now());
		user.updateNickname("oldNickname");
		when(userRepository.findById(any())).thenReturn(Optional.of(user));
		when(spotifyAccountRepository.findByUserId(any())).thenReturn(Optional.empty());

		UserProfileResponse response = userProfileService.updateMyProfile(
			1L, new UpdateUserProfileRequest(null, LocalDate.of(1999, 12, 31)));

		assertThat(response.nickname()).isEqualTo("oldNickname");
		assertThat(response.birthDate()).isEqualTo(LocalDate.of(1999, 12, 31));
	}

	@Test
	void trimsNicknameBeforeSaving() {
		User user = User.createActive(LocalDateTime.now());
		when(userRepository.findById(any())).thenReturn(Optional.of(user));
		when(spotifyAccountRepository.findByUserId(any())).thenReturn(Optional.empty());

		UserProfileResponse response = userProfileService.updateMyProfile(
			1L, new UpdateUserProfileRequest("  trimmed  ", null));

		assertThat(response.nickname()).isEqualTo("trimmed");
	}

	@Test
	void rejectsBlankNickname() {
		User user = User.createActive(LocalDateTime.now());
		when(userRepository.findById(any())).thenReturn(Optional.of(user));

		assertThatThrownBy(() -> userProfileService.updateMyProfile(
			1L, new UpdateUserProfileRequest("   ", null)))
			.isInstanceOf(BusinessException.class)
			.satisfies(exception ->
				assertThat(((BusinessException) exception).getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
	}
}
