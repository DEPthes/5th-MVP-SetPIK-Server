package com.setpik.server.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;
import java.time.ZoneId;

/** Spotify OAuth 환경변수를 타입 안전한 설정 객체로 등록한다. */
@Configuration
@EnableConfigurationProperties({SpotifyOAuthProperties.class, SetpikAuthProperties.class})
public class SpotifyOAuthConfig {

	@Bean
	public Clock clock() {
		// 팀원의 운영체제 시간대와 무관하게 서비스 시각을 한국 표준시로 통일한다.
		return Clock.system(ZoneId.of("Asia/Seoul"));
	}
}
