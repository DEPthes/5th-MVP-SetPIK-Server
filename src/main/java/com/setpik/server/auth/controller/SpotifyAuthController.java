package com.setpik.server.auth.controller;

import com.setpik.server.auth.dto.SpotifyLoginUrlResponse;
import com.setpik.server.auth.dto.SpotifyCallbackResult;
import com.setpik.server.auth.exception.SpotifyOAuthCallbackException;
import com.setpik.server.auth.service.SpotifyAuthService;
import com.setpik.server.auth.service.SpotifyOAuthCallbackService;
import com.setpik.server.auth.service.SpotifyOAuthFrontendRedirectService;
import com.setpik.server.auth.support.SpotifyOAuthFrontendCookieFactory;
import com.setpik.server.auth.support.SpotifyOAuthStateCookieFactory;
import com.setpik.server.auth.support.RefreshTokenCookieFactory;
import com.setpik.server.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/auth/spotify")
public class SpotifyAuthController {

	private final SpotifyAuthService spotifyAuthService;
	private final SpotifyOAuthCallbackService spotifyOAuthCallbackService;
	private final SpotifyOAuthStateCookieFactory stateCookieFactory;
	private final SpotifyOAuthFrontendCookieFactory frontendCookieFactory;
	private final SpotifyOAuthFrontendRedirectService frontendRedirectService;
	private final RefreshTokenCookieFactory refreshTokenCookieFactory;

	public SpotifyAuthController(
		SpotifyAuthService spotifyAuthService,
		SpotifyOAuthCallbackService spotifyOAuthCallbackService,
		SpotifyOAuthStateCookieFactory stateCookieFactory,
		SpotifyOAuthFrontendCookieFactory frontendCookieFactory,
		SpotifyOAuthFrontendRedirectService frontendRedirectService,
		RefreshTokenCookieFactory refreshTokenCookieFactory
	) {
		this.spotifyAuthService = spotifyAuthService;
		this.spotifyOAuthCallbackService = spotifyOAuthCallbackService;
		this.stateCookieFactory = stateCookieFactory;
		this.frontendCookieFactory = frontendCookieFactory;
		this.frontendRedirectService = frontendRedirectService;
		this.refreshTokenCookieFactory = refreshTokenCookieFactory;
	}

	@Operation(
		summary = "Spotify 로그인 URL 조회",
		description = "Spotify OAuth 인가 페이지로 이동하기 위한 URL과 state 값을 발급합니다."
	)
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200",
			description = "로그인 URL 발급 성공",
			content = @Content(
				mediaType = "application/json",
				examples = @ExampleObject(value = """
					{
					  "isSuccess": true,
					  "code": 1000,
					  "message": "요청에 성공했습니다.",
					  "result": {
					    "loginUrl": "https://accounts.spotify.com/authorize?...",
					    "state": "4c9a10f7-6b17-4d9a-b84e-e912e4ff3b50"
					  }
					}
					""")
			)
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "400",
			description = "요청 값 오류",
			content = @Content(
				mediaType = "application/json",
				examples = @ExampleObject(value = """
					{
					  "isSuccess": false,
					  "code": 2000,
					  "message": "요청 값이 올바르지 않습니다.",
					  "result": null
					}
					""")
			)
		)
	})
	@GetMapping("/login-url")
	public ResponseEntity<ApiResponse<SpotifyLoginUrlResponse>> getLoginUrl(
		@Parameter(description = "서버 허용 목록에 등록된 OAuth 콜백 URI", required = true)
		@RequestParam @NotBlank String redirectUri
	) {
		// Controller는 HTTP 요청과 쿠키, 공통 응답 조립만 담당한다.
		SpotifyLoginUrlResponse result = spotifyAuthService.createLoginUrl(redirectUri);

		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, stateCookieFactory.create(result.state()).toString())
			.body(ApiResponse.success(result));
	}

	@Operation(
		summary = "Spotify 로그인 시작",
		description = "state 쿠키를 설정한 뒤 Spotify OAuth 인가 페이지로 바로 이동합니다."
	)
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "302",
			description = "state 쿠키 설정 후 Spotify 로그인 페이지로 이동"
		)
	})
	@GetMapping("/login")
	public ResponseEntity<Void> login(
		@Parameter(description = "로그인 완료 후 돌아갈 허용된 프론트엔드 주소")
		@RequestParam(required = false) String frontendUrl
	) {
		SpotifyLoginUrlResponse result = spotifyAuthService.createLoginUrl();
		String frontendOrigin = frontendRedirectService.selectFrontendOrigin(frontendUrl);

		return ResponseEntity.status(HttpStatus.FOUND)
			.location(URI.create(result.loginUrl()))
			.header(HttpHeaders.SET_COOKIE, stateCookieFactory.create(result.state()).toString())
			.header(HttpHeaders.SET_COOKIE, frontendCookieFactory.create(frontendOrigin).toString())
			.header(HttpHeaders.CACHE_CONTROL, "no-store")
			.build();
	}

	@Operation(
		summary = "Spotify OAuth 콜백 처리",
		description = "Spotify 인증 코드를 처리하고 Refresh Token 쿠키 설정 후 프론트엔드로 리다이렉트합니다."
	)
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "302",
			description = "성공 페이지 또는 오류 코드가 포함된 실패 페이지로 이동"
		)
	})
	@GetMapping("/callback")
	public ResponseEntity<Void> callback(
		@RequestParam(required = false) String code,
		@RequestParam(required = false) String state,
		@RequestParam(required = false) String error,
		@CookieValue(name = "${spotify.oauth.state-cookie-name}", required = false) String storedState,
		@CookieValue(name = "${spotify.oauth.frontend-cookie-name}", required = false) String storedFrontend
	) {
		String frontendOrigin = frontendCookieFactory.decode(storedFrontend);
		// OAuth 콜백은 JSON 대신 명세서에 정의된 302 응답과 쿠키를 반환한다.
		try {
			SpotifyCallbackResult result = spotifyOAuthCallbackService
				.handleCallback(code, state, storedState, error);
			return ResponseEntity.status(HttpStatus.FOUND)
				.location(frontendRedirectService.buildSuccessUri(frontendOrigin))
				.header(HttpHeaders.SET_COOKIE, refreshTokenCookieFactory.create(result.refreshToken()).toString())
				.header(HttpHeaders.SET_COOKIE, stateCookieFactory.delete().toString())
				.header(HttpHeaders.SET_COOKIE, frontendCookieFactory.delete().toString())
				.header(HttpHeaders.CACHE_CONTROL, "no-store")
				.build();
		} catch (SpotifyOAuthCallbackException exception) {
			return ResponseEntity.status(HttpStatus.FOUND)
				.location(frontendRedirectService.buildFailureUri(
					frontendOrigin,
					exception.getErrorCode().getCode()
				))
				.header(HttpHeaders.SET_COOKIE, stateCookieFactory.delete().toString())
				.header(HttpHeaders.SET_COOKIE, frontendCookieFactory.delete().toString())
				.header(HttpHeaders.CACHE_CONTROL, "no-store")
				.build();
		}
	}

}
