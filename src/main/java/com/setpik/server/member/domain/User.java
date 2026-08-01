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
import java.time.LocalDateTime;

/** Flyway의 Users 테이블을 그대로 매핑한다. */
@Entity
@Table(name = "Users")
public class User extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 50)
	private UserStatus status;

	@Column(name = "last_login_at", nullable = true)
	private LocalDateTime lastLoginAt;

	protected User() {
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

}
