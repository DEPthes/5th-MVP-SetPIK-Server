package com.setpik.server.member.controller;

import com.setpik.server.common.api.ApiResponse;
import com.setpik.server.common.config.SwaggerConfig;
import com.setpik.server.common.exception.BusinessException;
import com.setpik.server.common.exception.ErrorCode;
import com.setpik.server.member.dto.UserProfileResponse;
import com.setpik.server.member.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

	private final UserProfileService userProfileService;

	public UserController(UserProfileService userProfileService) {
		this.userProfileService = userProfileService;
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

	private Long parseUserId(String subject) {
		try {
			return Long.valueOf(subject);
		} catch (NumberFormatException exception) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}
	}
}
