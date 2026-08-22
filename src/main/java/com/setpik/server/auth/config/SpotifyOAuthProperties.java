package com.setpik.server.auth.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "spotify.oauth")
public record SpotifyOAuthProperties(
	@NotBlank String authorizationUri,
	@NotBlank String clientId,
	@NotBlank String clientSecret,
	@NotBlank String redirectUri,
	@NotEmpty List<@NotBlank String> scopes,
	@NotBlank String stateCookieName,
	@NotBlank String frontendCookieName,
	@NotNull Duration stateCookieMaxAge,
	boolean secureCookie
) {
}
