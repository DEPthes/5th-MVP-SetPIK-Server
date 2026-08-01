package com.setpik.server.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ApiResponseTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void successResponseUsesApiSpecificationFieldNames() throws Exception {
		ApiResponse<String> response = ApiResponse.success("sample");

		JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(response));

		assertThat(json.get("isSuccess").asBoolean()).isTrue();
		assertThat(json.get("code").asInt()).isEqualTo(1000);
		assertThat(json.get("message").asText()).isEqualTo("요청에 성공했습니다.");
		assertThat(json.get("result").asText()).isEqualTo("sample");
	}
}
