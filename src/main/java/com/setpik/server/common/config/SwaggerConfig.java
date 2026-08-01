package com.setpik.server.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

	public static final String BEARER_AUTH = "bearerAuth";

	@Bean
	public OpenAPI setpikOpenApi() {
		// 인증이 필요한 Controller에만 @SecurityRequirement를 붙여 공개 API와 구분한다.
		return new OpenAPI()
			.info(new Info()
				.title("SetPIK API")
				.description("SetPIK 백엔드 API 문서")
				.version("v1"))
			.components(new Components().addSecuritySchemes(BEARER_AUTH,
				new SecurityScheme()
					.name(BEARER_AUTH)
					.type(SecurityScheme.Type.HTTP)
					.scheme("bearer")
					.bearerFormat("JWT")));
	}
}
