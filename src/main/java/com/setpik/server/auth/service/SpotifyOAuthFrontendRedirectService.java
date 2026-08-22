package com.setpik.server.auth.service;

import com.setpik.server.auth.config.SetpikAuthProperties;
import com.setpik.server.common.exception.BusinessException;
import com.setpik.server.common.exception.ErrorCode;
import java.net.URI;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class SpotifyOAuthFrontendRedirectService {

	private static final String SUCCESS_PATH = "/oauth/success";
	private static final String FAILURE_PATH = "/oauth/failure";

	private final List<String> allowedOrigins;
	private final SetpikAuthProperties authProperties;

	public SpotifyOAuthFrontendRedirectService(
		@Value("${setpik.cors.allowed-origins}") List<String> allowedOrigins,
		SetpikAuthProperties authProperties
	) {
		this.allowedOrigins = allowedOrigins.stream().map(this::normalizeOrigin).toList();
		this.authProperties = authProperties;
	}

	/** 로그인 시작 환경을 허용 목록으로 제한해 임의 사이트로의 리다이렉트를 방지한다. */
	public String selectFrontendOrigin(String requestedFrontendUrl) {
		if (!StringUtils.hasText(requestedFrontendUrl)) {
			return originOf(authProperties.successRedirectUri());
		}

		String origin = normalizeOrigin(requestedFrontendUrl);
		if (!allowedOrigins.contains(origin)) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}
		return origin;
	}

	public URI buildSuccessUri(String storedFrontendOrigin) {
		if (!StringUtils.hasText(storedFrontendOrigin)) {
			return URI.create(authProperties.successRedirectUri());
		}
		return URI.create(resolveAllowedStoredOrigin(storedFrontendOrigin) + SUCCESS_PATH);
	}

	public URI buildFailureUri(String storedFrontendOrigin, int errorCode) {
		String failureUri;
		if (StringUtils.hasText(storedFrontendOrigin)) {
			failureUri = resolveAllowedStoredOrigin(storedFrontendOrigin) + FAILURE_PATH;
		} else {
			failureUri = authProperties.failureRedirectUri();
		}

		return UriComponentsBuilder.fromUriString(failureUri)
			.queryParam("code", errorCode)
			.build()
			.toUri();
	}

	private String resolveAllowedStoredOrigin(String storedFrontendOrigin) {
		try {
			String origin = normalizeOrigin(storedFrontendOrigin);
			return allowedOrigins.contains(origin) ? origin : originOf(authProperties.successRedirectUri());
		} catch (BusinessException exception) {
			return originOf(authProperties.successRedirectUri());
		}
	}

	private String originOf(String uriValue) {
		URI uri = parseUri(uriValue);
		return uri.getScheme() + "://" + uri.getAuthority();
	}

	private String normalizeOrigin(String uriValue) {
		URI uri = parseUri(uriValue.trim());
		String path = uri.getPath();
		if (uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
			|| (StringUtils.hasText(path) && !"/".equals(path))) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}
		return uri.getScheme().toLowerCase() + "://" + uri.getAuthority().toLowerCase();
	}

	private URI parseUri(String uriValue) {
		try {
			URI uri = URI.create(uriValue);
			if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
				|| !StringUtils.hasText(uri.getAuthority())) {
				throw new BusinessException(ErrorCode.INVALID_REQUEST);
			}
			return uri;
		} catch (IllegalArgumentException exception) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}
	}
}
