package com.jjonggeut.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 내 계정 수정 요청. 보낸 항목만 바뀌고 생략한 항목은 그대로다.
 * phone, email 은 빈 문자열("")을 보내면 값을 지운다. 이름과 닉네임은 지울 수 없다.
 */
public record UserUpdateRequest(

		@Pattern(regexp = "^[가-힣a-zA-Z]{2,20}$", message = "이름은 2~20자의 한글 또는 영문만 사용할 수 있습니다.")
		String name,

		@Pattern(regexp = "^[가-힣a-zA-Z0-9]{2,10}$", message = "닉네임은 2~10자의 한글, 영문, 숫자만 사용할 수 있습니다.")
		String nickname,

		@Pattern(regexp = "^(01[0-9]-?\\d{3,4}-?\\d{4})?$", message = "연락처 형식이 올바르지 않습니다. 예: 010-1234-5678")
		String phone,

		@Email(message = "이메일 형식이 올바르지 않습니다.")
		@Size(max = 255, message = "이메일은 255자 이하여야 합니다.")
		String email
) {

	@JsonIgnore
	@AssertTrue(message = "수정할 항목을 하나 이상 보내야 합니다.")
	public boolean isAnyFieldPresent() {
		return name != null || nickname != null || phone != null || email != null;
	}
}
