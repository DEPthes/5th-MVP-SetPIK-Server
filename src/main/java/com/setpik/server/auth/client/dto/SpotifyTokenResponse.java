package com.setpik.server.auth.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SpotifyTokenResponse(
	@JsonProperty("access_token") String accessToken,
	@JsonProperty("token_type") String tokenType,
	String scope,
	@JsonProperty("expires_in") long expiresIn,
	@JsonProperty("refresh_token") String refreshToken
) {
}
