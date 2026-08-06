package com.setpik.server.auth.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "setpik.auth")
public record SetpikAuthProperties(
	@NotBlank String tokenEncryptionKey,
	@NotNull Duration refreshTokenExpiration,
	@NotBlank String refreshCookieName,
	@NotBlank String refreshCookiePath,
	boolean secureCookie,
	@NotBlank String successRedirectUri,
	@NotBlank String failureRedirectUri
) {
}
