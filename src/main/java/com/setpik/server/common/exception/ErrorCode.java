package com.setpik.server.common.exception;

import org.springframework.http.HttpStatus;

/** HTTP 상태와 SetPIK 애플리케이션 코드를 한곳에서 관리한다. */
public enum ErrorCode {

	INVALID_REQUEST(HttpStatus.BAD_REQUEST, 2000, "요청 값이 올바르지 않습니다."),
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, 2001, "인증에 실패했습니다."),
	FORBIDDEN(HttpStatus.FORBIDDEN, 2002, "접근 권한이 없습니다."),
	RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, 2003, "요청한 리소스를 찾을 수 없습니다."),
	DUPLICATE_REQUEST(HttpStatus.CONFLICT, 2004, "중복된 요청입니다."),
	SPOTIFY_CONNECTION_REQUIRED(HttpStatus.CONFLICT, 2100, "Spotify 계정 연동이 필요합니다."),
	SPOTIFY_REAUTHENTICATION_REQUIRED(HttpStatus.CONFLICT, 2101, "Spotify 재인증이 필요합니다."),
	SPOTIFY_API_ERROR(HttpStatus.BAD_GATEWAY, 2200, "Spotify API 처리에 실패했습니다."),
	ANALYSIS_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 2300, "플레이리스트 분석에 실패했습니다."),
	PERFORMANCE_MATCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 2400, "공연 매칭에 실패했습니다."),
	PRESTUDY_PLAYLIST_CREATION_FAILED(HttpStatus.BAD_GATEWAY, 2600, "예습 플레이리스트 생성에 실패했습니다."),
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 3000, "서버 내부 오류가 발생했습니다.");

	private final HttpStatus httpStatus;
	private final int code;
	private final String message;

	ErrorCode(HttpStatus httpStatus, int code, String message) {
		this.httpStatus = httpStatus;
		this.code = code;
		this.message = message;
	}

	public HttpStatus getHttpStatus() {
		return httpStatus;
	}

	public int getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}
}
