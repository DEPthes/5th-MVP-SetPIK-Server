package com.setpik.server.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SpotifyAuthControllerTest {

	private static final String LOGIN_URL = "/api/v1/auth/spotify/login-url";
	private static final String LOGIN = "/api/v1/auth/spotify/login";
	private static final String REDIRECT_URI = "http://localhost:8080/api/v1/auth/spotify/callback";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void returnsLoginUrlAndStateUsingCommonResponseFormat() throws Exception {
		MvcResult mvcResult = mockMvc.perform(get(LOGIN_URL).param("redirectUri", REDIRECT_URI))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.code").value(1000))
			.andExpect(jsonPath("$.message").value("요청에 성공했습니다."))
			.andExpect(jsonPath("$.result.loginUrl").isString())
			.andExpect(jsonPath("$.result.state").isString())
			.andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("HttpOnly")))
			.andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("SameSite=Lax")))
			.andReturn();

		JsonNode responseBody = objectMapper.readTree(mvcResult.getResponse().getContentAsString());
		String state = responseBody.at("/result/state").asText();
		String loginUrl = responseBody.at("/result/loginUrl").asText();
		String setCookie = mvcResult.getResponse().getHeader(HttpHeaders.SET_COOKIE);

		assertThat(loginUrl).contains("state=" + state);
		assertThat(setCookie).contains("setpik_spotify_oauth_state=" + state);
	}

	@Test
	void setsStateCookieAndRedirectsBrowserToSpotify() throws Exception {
		mockMvc.perform(get(LOGIN))
			.andExpect(status().isFound())
			.andExpect(header().string(HttpHeaders.LOCATION,
				org.hamcrest.Matchers.startsWith("https://accounts.spotify.com/authorize?")))
			.andExpect(header().string(HttpHeaders.SET_COOKIE,
				org.hamcrest.Matchers.containsString("setpik_spotify_oauth_state=")))
			.andExpect(header().string(HttpHeaders.SET_COOKIE,
				org.hamcrest.Matchers.containsString("HttpOnly")))
			.andExpect(header().string(HttpHeaders.SET_COOKIE,
				org.hamcrest.Matchers.containsString("SameSite=Lax")))
			.andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"));
	}

	@Test
	void returnsBadRequestWhenRedirectUriIsMissing() throws Exception {
		mockMvc.perform(get(LOGIN_URL))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.isSuccess").value(false))
			.andExpect(jsonPath("$.code").value(2000));
	}

	@Test
	void returnsBadRequestWhenRedirectUriIsNotAllowed() throws Exception {
		mockMvc.perform(get(LOGIN_URL).param("redirectUri", "https://attacker.example/callback"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.isSuccess").value(false))
			.andExpect(jsonPath("$.code").value(2000));
	}
}
