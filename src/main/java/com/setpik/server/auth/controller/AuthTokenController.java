package com.setpik.server.auth.controller;

import com.setpik.server.auth.dto.AccessTokenResponse;
import com.setpik.server.auth.service.AuthTokenService;
import com.setpik.server.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/token")
public class AuthTokenController {

	private final AuthTokenService authTokenService;

	public AuthTokenController(AuthTokenService authTokenService) {
		this.authTokenService = authTokenService;
	}

	@Operation(
		summary = "Access Token 재발급",
		description = "HttpOnly 쿠키의 Refresh Token을 검증하고 새 Access Token을 발급합니다."
	)
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200",
			description = "Access Token 재발급 성공",
			content = @Content(
				mediaType = "application/json",
				examples = @ExampleObject(value = """
					{
					  "isSuccess": true,
					  "code": 1000,
					  "message": "Access Token이 재발급되었습니다.",
					  "result": {
					    "accessToken": "new-jwt-access-token"
					  }
					}
					""")
			)
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "401",
			description = "Refresh Token 누락·만료·위조"
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "404",
			description = "회원 없음"
		)
	})
	@PostMapping("/refresh")
	public ResponseEntity<ApiResponse<AccessTokenResponse>> refresh(
		@Parameter(hidden = true)
		@CookieValue(name = "${setpik.auth.refresh-cookie-name}", required = false)
		String refreshToken
	) {
		// Controller는 쿠키를 전달하고 명세서의 공통 응답만 조립한다.
		AccessTokenResponse result = authTokenService.reissueAccessToken(refreshToken);
		return ResponseEntity.ok(ApiResponse.success("Access Token이 재발급되었습니다.", result));
	}
}
