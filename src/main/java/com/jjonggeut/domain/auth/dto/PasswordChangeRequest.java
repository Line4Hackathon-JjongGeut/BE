package com.jjonggeut.domain.auth.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record PasswordChangeRequest(

		/** 로그인 상태에서 변경할 때 필수 */
		String currentPassword,

		/** 비밀번호 재설정 요청으로 받은 토큰. 로그인하지 않은 상태에서 변경할 때 필수 */
		String resetToken,

		@NotNull(message = "새 비밀번호를 입력해주세요.")
		@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)\\S{8,20}$", message = "비밀번호는 8~20자이며 영문과 숫자를 각각 1자 이상 포함해야 합니다.")
		String newPassword
) {

	/** 비밀번호와 토큰이 로그에 남지 않도록 마스킹한다. */
	@Override
	public String toString() {
		return "PasswordChangeRequest[currentPassword=****, resetToken=****, newPassword=****]";
	}
}
