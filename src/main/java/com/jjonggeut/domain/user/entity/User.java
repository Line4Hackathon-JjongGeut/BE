package com.jjonggeut.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "user_id")
	private Long userId;

	@Column(name = "login_id", nullable = false, length = 50, unique = true)
	private String loginId;

	@Column(length = 255)
	private String email;

	@Column(name = "password_hash", nullable = false, length = 255)
	private String passwordHash;

	@Column(nullable = false, length = 50)
	private String name;

	@Column(length = 50, unique = true)
	private String nickname;

	@Column(length = 20)
	private String phone;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Role role;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private UserStatus status;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	public static User createUser(String loginId, String encodedPassword, String name, String nickname) {
		User user = new User();
		user.loginId = loginId;
		user.passwordHash = encodedPassword;
		user.name = name;
		user.nickname = nickname;
		user.role = Role.USER;
		user.status = UserStatus.ACTIVE;
		return user;
	}

	public void changePassword(String encodedPassword) {
		this.passwordHash = encodedPassword;
	}
}
