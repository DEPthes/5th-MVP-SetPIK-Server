package com.setpik.server.common.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.stereotype.Component;

@Component
public class ApiBearerTokenResolver implements BearerTokenResolver {

	private static final String LOGOUT_PATH = "/api/v1/auth/logout";
	private final DefaultBearerTokenResolver delegate = new DefaultBearerTokenResolver();

	@Override
	public String resolve(HttpServletRequest request) {
		// 로그아웃은 만료된 Access Token이 있어도 Refresh Token 폐기를 계속 진행한다.
		String logoutRequestUri = request.getContextPath() + LOGOUT_PATH;
		if (logoutRequestUri.equals(request.getRequestURI())) {
			return null;
		}
		return delegate.resolve(request);
	}
}
