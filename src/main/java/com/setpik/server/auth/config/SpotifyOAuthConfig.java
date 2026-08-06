package com.setpik.server.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;

/** Spotify OAuth 환경변수를 타입 안전한 설정 객체로 등록한다. */
@Configuration
@EnableConfigurationProperties({SpotifyOAuthProperties.class, SetpikAuthProperties.class})
public class SpotifyOAuthConfig {

	@Bean
	public Clock clock() {
		return Clock.systemDefaultZone();
	}
}
