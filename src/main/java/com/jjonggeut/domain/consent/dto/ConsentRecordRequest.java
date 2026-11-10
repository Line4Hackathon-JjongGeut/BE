package com.jjonggeut.domain.consent.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jjonggeut.domain.consent.entity.ConsentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Objects;

/** 동의 기록 요청. 한 번에 여러 종류를 보낼 수 있다. */
public record ConsentRecordRequest(

		@NotEmpty(message = "기록할 동의를 1개 이상 입력해주세요.")
		@Size(max = 5, message = "동의는 한 번에 최대 5개까지 기록할 수 있습니다.")
		List<@NotNull(message = "동의 항목이 비어 있습니다.") @Valid ConsentItem> consents
) {

	/** 같은 종류를 한 요청에 두 번 보낼 수 없다. */
	@JsonIgnore
	@AssertTrue(message = "같은 종류의 동의를 한 번에 두 번 기록할 수 없습니다.")
	public boolean isNoDuplicateType() {
		if (consents == null) {
			return true;
		}
		List<ConsentType> types = consents.stream()
				.filter(Objects::nonNull)
				.map(ConsentItem::consentType)
				.filter(Objects::nonNull)
				.toList();
		return types.size() == types.stream().distinct().count();
	}

	public record ConsentItem(

			@NotNull(message = "동의 종류를 입력해주세요.")
			ConsentType consentType,

			@NotBlank(message = "정책 버전을 입력해주세요.")
			@Size(max = 20, message = "정책 버전은 20자 이하로 입력해주세요.")
			String policyVersion,

			@NotNull(message = "동의 여부를 입력해주세요.")
			Boolean agreed
	) {
	}
}
