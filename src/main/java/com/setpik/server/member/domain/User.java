package com.setpik.server.member.domain;

import com.setpik.server.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Flyway의 Users 테이블을 그대로 매핑한다. */
@Entity
@Table(name = "Users")
public class User extends BaseEntity {

	// TODO: 기본 프로필 이미지 URL이 정해지면 이 값으로 교체한다.
	private static final String DEFAULT_PROFILE_IMAGE_URL = null;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 50)
	private UserStatus status;

	@Column(name = "last_login_at", nullable = true)
	private LocalDateTime lastLoginAt;

	@Column(name = "nickname", nullable = true, length = 20)
	private String nickname;

	@Column(name = "birth_date", nullable = true)
	private LocalDate birthDate;

	@Column(name = "profile_image_url", nullable = true, length = 2048)
	private String profileImageUrl;

	protected User() {
	}

	/** Spotify 최초 로그인 시 활성 회원을 생성한다. */
	public static User createActive(LocalDateTime loginAt) {
		User user = new User();
		user.status = UserStatus.ACTIVE;
		user.lastLoginAt = loginAt;
		user.profileImageUrl = DEFAULT_PROFILE_IMAGE_URL;
		return user;
	}

	/** 기존 회원이 다시 로그인한 시각을 갱신한다. */
	public void recordLogin(LocalDateTime loginAt) {
		this.lastLoginAt = loginAt;
	}

	/** 회원 계정을 탈퇴 상태로 전환한다. */
	public void withdraw() {
		this.status = UserStatus.WITHDRAWN;
	}

	/** 닉네임을 변경한다. */
	public void updateNickname(String nickname) {
		this.nickname = nickname;
	}

	/** 생년월일을 변경한다. */
	public void updateBirthDate(LocalDate birthDate) {
		this.birthDate = birthDate;
	}

	/** 업로드된 프로필 이미지 URL로 교체한다. */
	public void updateProfileImageUrl(String profileImageUrl) {
		this.profileImageUrl = profileImageUrl;
	}

	/** 프로필 이미지를 기본 이미지로 초기화한다. */
	public void resetProfileImageUrl() {
		this.profileImageUrl = DEFAULT_PROFILE_IMAGE_URL;
	}

	public Long getUserId() {
		return userId;
	}

	public UserStatus getStatus() {
		return status;
	}

	public LocalDateTime getLastLoginAt() {
		return lastLoginAt;
	}

	public String getNickname() {
		return nickname;
	}

	public LocalDate getBirthDate() {
		return birthDate;
	}

	public String getProfileImageUrl() {
		return profileImageUrl;
	}

}
