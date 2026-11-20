package com.jjonggeut.domain.auth.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record SignupRequest(

		@NotNull(message = "이름을 입력해주세요.")
		@Pattern(regexp = "^[가-힣a-zA-Z]{2,20}$", message = "이름은 2~20자의 한글 또는 영문만 사용할 수 있습니다.")
		String name,

		@NotNull(message = "닉네임을 입력해주세요.")
		@Pattern(regexp = "^[가-힣a-zA-Z0-9]{2,10}$", message = "닉네임은 2~10자의 한글, 영문, 숫자만 사용할 수 있습니다.")
		String nickname,

		@NotNull(message = "아이디를 입력해주세요.")
		@Pattern(regexp = "^[a-z0-9]{4,20}$", message = "아이디는 4~20자의 영문 소문자와 숫자만 사용할 수 있습니다.")
		String loginId,

		@NotNull(message = "비밀번호를 입력해주세요.")
		@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)\\S{8,20}$", message = "비밀번호는 8~20자이며 영문과 숫자를 각각 1자 이상 포함해야 합니다.")
		String password
) {

	/** 비밀번호가 로그에 남지 않도록 마스킹한다. */
	@Override
	public String toString() {
		return "SignupRequest[name=" + name + ", nickname=" + nickname + ", loginId=" + loginId + ", password=****]";
	}
}
