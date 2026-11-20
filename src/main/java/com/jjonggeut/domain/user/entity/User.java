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

	@Column(name = "profile_image_url", length = 500)
	private String profileImageUrl;

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

	public void changeName(String name) {
		this.name = name;
	}

	public void changeNickname(String nickname) {
		this.nickname = nickname;
	}

	public void changePhone(String phone) {
		this.phone = phone;
	}

	public void changeProfileImageUrl(String profileImageUrl) {
		this.profileImageUrl = profileImageUrl;
	}

	public void changeEmail(String email) {
		this.email = email;
	}

	/**
	 * 탈퇴 처리: 개인정보를 지우고 로그인할 수 없는 상태로 바꾼다.
	 * login_id 는 UNIQUE NOT NULL 이라 지우지 않고 자리표시자로 바꿔 아이디와 닉네임을 다시 쓸 수 있게 한다.
	 */
	public void withdraw(String unusablePasswordHash, LocalDateTime now) {
		this.loginId = "withdrawn_" + this.userId;
		this.passwordHash = unusablePasswordHash;
		this.name = "탈퇴한 사용자";
		this.nickname = null;
		this.email = null;
		this.phone = null;
		this.profileImageUrl = null;
		this.status = UserStatus.WITHDRAWN;
		this.deletedAt = now;
	}
}
