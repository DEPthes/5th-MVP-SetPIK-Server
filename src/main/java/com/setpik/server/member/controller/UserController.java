package com.setpik.server.member.controller;

import com.setpik.server.common.api.ApiResponse;
import com.setpik.server.common.config.SwaggerConfig;
import com.setpik.server.common.exception.BusinessException;
import com.setpik.server.common.exception.ErrorCode;
import com.setpik.server.member.dto.ProfileImageResponse;
import com.setpik.server.member.dto.UpdateUserProfileRequest;
import com.setpik.server.member.dto.UserProfileResponse;
import com.setpik.server.member.dto.OnboardingStatusResponse;
import com.setpik.server.member.service.OnboardingService;
import com.setpik.server.member.service.UserProfileService;
import com.setpik.server.member.service.UserWithdrawalService;
import com.setpik.server.spotify.dto.SpotifyConnectionResponse;
import com.setpik.server.spotify.service.SpotifyConnectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

	private final UserProfileService userProfileService;
	private final UserWithdrawalService userWithdrawalService;
	private final SpotifyConnectionService spotifyConnectionService;
	private final OnboardingService onboardingService;

	public UserController(
		UserProfileService userProfileService,
		UserWithdrawalService userWithdrawalService,
		SpotifyConnectionService spotifyConnectionService,
		OnboardingService onboardingService
	) {
		this.userProfileService = userProfileService;
		this.userWithdrawalService = userWithdrawalService;
		this.spotifyConnectionService = spotifyConnectionService;
		this.onboardingService = onboardingService;
	}

	@Operation(
		summary = "회원 탈퇴",
		description = "회원 계정을 탈퇴 처리하고 인증 정보와 연관 리소스를 비활성화합니다."
	)
	@SecurityRequirement(name = SwaggerConfig.BEARER_AUTH)
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200",
			description = "회원 탈퇴 성공",
			content = @Content(
				mediaType = "application/json",
				examples = @ExampleObject(value = """
					{
					  "isSuccess": true,
					  "code": 1000,
					  "message": "회원 탈퇴가 완료되었습니다.",
					  "result": null
					}
					""")
			)
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "400",
			description = "요청 값 오류"
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "401",
			description = "인증 실패"
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "409",
			description = "이미 탈퇴한 회원"
		)
	})
	@DeleteMapping("/me")
	public ResponseEntity<ApiResponse<Void>> withdraw(
		@AuthenticationPrincipal Jwt jwt
	) {
		Long userId = parseUserId(jwt.getSubject());
		userWithdrawalService.withdraw(userId);
		return ResponseEntity.ok(ApiResponse.success("회원 탈퇴가 완료되었습니다.", null));
	}

	@Operation(
		summary = "내 프로필 조회",
		description = "로그인한 회원의 기본 프로필, 최근 로그인 시간, Spotify 연동 정보를 조회합니다."
	)
	@SecurityRequirement(name = SwaggerConfig.BEARER_AUTH)
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200",
			description = "내 프로필 조회 성공",
			content = @Content(
				mediaType = "application/json",
				examples = @ExampleObject(value = """
					{
					  "isSuccess": true,
					  "code": 1000,
					  "message": "요청에 성공했습니다.",
					  "result": {
					    "userId": 1,
					    "status": "ACTIVE",
					    "lastLoginAt": "2026-07-28T09:10:11+09:00",
					    "nickname": "setpik_user",
					    "birthDate": "2000-01-01",
					    "profileImageUrl": null,
					    "spotifyConnected": true,
					    "spotifyAccount": {
					      "spotifyUserId": "31abcde",
					      "displayName": "setpik_user",
					      "profileImageUrl": "https://i.scdn.co/image/abc123"
					    }
					  }
					}
					""")
			)
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "400",
			description = "요청 값 오류"
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "401",
			description = "인증 실패"
		)
	})
	@GetMapping("/me")
	public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(
		@AuthenticationPrincipal Jwt jwt
	) {
		// Controller는 검증이 끝난 JWT에서 사용자 식별자만 추출한다.
		Long userId = parseUserId(jwt.getSubject());
		return ResponseEntity.ok(ApiResponse.success(userProfileService.getMyProfile(userId)));
	}

	@Operation(
		summary = "내 프로필 수정",
		description = "닉네임과 생년월일을 각각 개별적으로 수정합니다. 요청에 포함되지 않은 필드는 기존 값을 유지합니다."
	)
	@SecurityRequirement(name = SwaggerConfig.BEARER_AUTH)
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200",
			description = "내 프로필 수정 성공",
			content = @Content(
				mediaType = "application/json",
				examples = @ExampleObject(value = """
					{
					  "isSuccess": true,
					  "code": 1000,
					  "message": "요청에 성공했습니다.",
					  "result": {
					    "userId": 1,
					    "status": "ACTIVE",
					    "lastLoginAt": "2026-07-28T09:10:11+09:00",
					    "nickname": "setpik_user",
					    "birthDate": "2000-01-01",
					    "profileImageUrl": null,
					    "spotifyConnected": true,
					    "spotifyAccount": {
					      "spotifyUserId": "31abcde",
					      "displayName": "setpik_user",
					      "profileImageUrl": "https://i.scdn.co/image/abc123"
					    }
					  }
					}
					""")
			)
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "400",
			description = "요청 값 오류"
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "401",
			description = "인증 실패"
		)
	})
	@PatchMapping("/me")
	public ResponseEntity<ApiResponse<UserProfileResponse>> updateMyProfile(
		@AuthenticationPrincipal Jwt jwt,
		@Valid @RequestBody UpdateUserProfileRequest request
	) {
		Long userId = parseUserId(jwt.getSubject());
		return ResponseEntity.ok(ApiResponse.success(userProfileService.updateMyProfile(userId, request)));
	}

	@Operation(
		summary = "프로필 이미지 업로드",
		description = "프로필 이미지를 업로드하고 교체된 이미지 URL을 반환합니다. "
			+ "TODO: 현재는 S3 연동 전이라 NoOpImageStorageClient가 가짜 URL만 반환합니다."
	)
	@SecurityRequirement(name = SwaggerConfig.BEARER_AUTH)
	@PutMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ApiResponse<ProfileImageResponse>> updateProfileImage(
		@AuthenticationPrincipal Jwt jwt,
		@RequestParam("image") MultipartFile image
	) {
		Long userId = parseUserId(jwt.getSubject());
		return ResponseEntity.ok(ApiResponse.success(userProfileService.updateProfileImage(userId, image)));
	}

	@Operation(
		summary = "프로필 이미지 초기화",
		description = "프로필 이미지를 기본 이미지로 되돌립니다."
	)
	@SecurityRequirement(name = SwaggerConfig.BEARER_AUTH)
	@DeleteMapping("/me/profile-image")
	public ResponseEntity<ApiResponse<ProfileImageResponse>> resetProfileImage(
		@AuthenticationPrincipal Jwt jwt
	) {
		Long userId = parseUserId(jwt.getSubject());
		return ResponseEntity.ok(ApiResponse.success(userProfileService.resetProfileImage(userId)));
	}

	@Operation(
		summary = "Spotify 연동 상태 조회",
		description = "Spotify 연결 상태, 토큰 만료 시각, 동의한 scope 목록을 조회합니다."
	)
	@SecurityRequirement(name = SwaggerConfig.BEARER_AUTH)
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200",
			description = "Spotify 연동 상태 조회 성공",
			content = @Content(
				mediaType = "application/json",
				examples = @ExampleObject(value = """
					{
					  "isSuccess": true,
					  "code": 1000,
					  "message": "요청에 성공했습니다.",
					  "result": {
					    "connected": true,
					    "connectionStatus": "CONNECTED",
					    "tokenExpiresAt": "2026-07-28T11:00:00+09:00",
					    "scopes": [
					      {
					        "scopeName": "user-read-email",
					        "isGranted": true
					      },
					      {
					        "scopeName": "playlist-read-private",
					        "isGranted": true
					      }
					    ]
					  }
					}
					""")
			)
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "400",
			description = "요청 값 오류"
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "401",
			description = "인증 실패"
		)
	})
	@GetMapping("/me/spotify-connection")
	public ResponseEntity<ApiResponse<SpotifyConnectionResponse>> getSpotifyConnection(
		@AuthenticationPrincipal Jwt jwt
	) {
		Long userId = parseUserId(jwt.getSubject());
		return ResponseEntity.ok(ApiResponse.success(
			spotifyConnectionService.getConnection(userId)
		));
	}

	@Operation(
		summary = "온보딩 상태 조회",
		description = "최근 플레이리스트 선택과 관심 아티스트 선택 완료 여부를 조회합니다."
	)
	@SecurityRequirement(name = SwaggerConfig.BEARER_AUTH)
	@GetMapping("/me/onboarding-status")
	public ResponseEntity<ApiResponse<OnboardingStatusResponse>> getOnboardingStatus(
		@AuthenticationPrincipal Jwt jwt
	) {
		Long userId = parseUserId(jwt.getSubject());
		return ResponseEntity.ok(ApiResponse.success(onboardingService.getStatus(userId)));
	}

	private Long parseUserId(String subject) {
		try {
			return Long.valueOf(subject);
		} catch (NumberFormatException exception) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}
	}
}
