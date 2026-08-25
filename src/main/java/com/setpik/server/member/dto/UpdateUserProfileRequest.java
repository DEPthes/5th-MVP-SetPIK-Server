package com.setpik.server.member.dto;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** nickname, birthDate는 각각 개별적으로 수정 가능하도록 둘 다 optional이다. */
public record UpdateUserProfileRequest(
	@Size(max = 20, message = "닉네임은 20자 이하로 입력해주세요.") String nickname,
	LocalDate birthDate
) {
}
