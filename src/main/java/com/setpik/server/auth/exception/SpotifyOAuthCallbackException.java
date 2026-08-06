package com.setpik.server.auth.exception;

import com.setpik.server.common.exception.ErrorCode;

/** OAuth 콜백에서 302 실패 리다이렉트로 변환해야 하는 오류다. */
public class SpotifyOAuthCallbackException extends RuntimeException {

	private final ErrorCode errorCode;

	public SpotifyOAuthCallbackException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}

	public ErrorCode getErrorCode() {
		return errorCode;
	}
}
