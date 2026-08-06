package com.setpik.server.auth.security;

import com.setpik.server.auth.config.JwtProperties;
import java.time.Clock;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Component;

@Component
public class JwtAccessTokenProvider {

	private final JwtEncoder jwtEncoder;
	private final JwtProperties properties;
	private final Clock clock;

	public JwtAccessTokenProvider(
		JwtEncoder jwtEncoder,
		JwtProperties properties,
		Clock clock
	) {
		this.jwtEncoder = jwtEncoder;
		this.properties = properties;
		this.clock = clock;
	}

	/** 인증 필터가 사용자 식별에 사용할 userId를 subject에 담아 Access Token을 발급한다. */
	public String issue(Long userId) {
		Instant issuedAt = clock.instant();
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer(properties.issuer())
			.subject(userId.toString())
			.issuedAt(issuedAt)
			.expiresAt(issuedAt.plus(properties.accessTokenExpiration()))
			.claim("tokenType", "access")
			.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256)
			.type("JWT")
			.build();
		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}
}
