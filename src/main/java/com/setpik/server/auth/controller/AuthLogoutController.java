package com.setpik.server.auth.controller;

import com.setpik.server.auth.service.AuthLogoutService;
import com.setpik.server.auth.support.RefreshTokenCookieFactory;
import com.setpik.server.common.api.ApiResponse;
import com.setpik.server.common.exception.BusinessException;
import com.setpik.server.common.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthLogoutController {

	private final AuthLogoutService authLogoutService;
	private final RefreshTokenCookieFactory refreshTokenCookieFactory;

	public AuthLogoutController(
		AuthLogoutService authLogoutService,
		RefreshTokenCookieFactory refreshTokenCookieFactory
	) {
		this.authLogoutService = authLogoutService;
		this.refreshTokenCookieFactory = refreshTokenCookieFactory;
	}

	@Operation(
		summary = "로그아웃",
		description = "서버에 저장된 Refresh Token을 폐기하고 인증 쿠키를 만료시킵니다."
	)
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200",
			description = "로그아웃 성공",
			content = @Content(
				mediaType = "application/json",
				examples = @ExampleObject(value = """
					{
					  "isSuccess": true,
					  "code": 1000,
					  "message": "로그아웃되었습니다.",
					  "result": null
					}
					""")
			)
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "400",
			description = "Refresh Token 쿠키 누락 또는 잘못된 요청"
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "409",
			description = "이미 폐기됐거나 만료된 Refresh Token"
		)
	})
	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<Void>> logout(
		@Parameter(hidden = true)
		@CookieValue(name = "${setpik.auth.refresh-cookie-name}", required = false)
		String refreshToken
	) {
		// Controller는 쿠키를 전달하고 성공 시 브라우저 쿠키를 즉시 만료시킨다.
		authLogoutService.logout(refreshToken);
		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, refreshTokenCookieFactory.delete().toString())
			.body(ApiResponse.success("로그아웃되었습니다.", null));
	}

	/** 로그아웃 실패 응답에서도 남아 있는 인증 쿠키는 제거한다. */
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse<Void>> handleLogoutFailure(BusinessException exception) {
		ErrorCode errorCode = exception.getErrorCode();
		return ResponseEntity.status(errorCode.getHttpStatus())
			.header(HttpHeaders.SET_COOKIE, refreshTokenCookieFactory.delete().toString())
			.body(ApiResponse.failure(errorCode, null));
	}
}
