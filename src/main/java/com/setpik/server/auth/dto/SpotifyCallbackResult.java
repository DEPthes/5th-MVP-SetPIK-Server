package com.setpik.server.auth.dto;

/** DB 저장이 끝난 뒤 Controller가 쿠키로 전달할 SetPIK Refresh Token이다. */
public record SpotifyCallbackResult(String refreshToken) {
}
