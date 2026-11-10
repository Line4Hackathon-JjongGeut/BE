package com.jjonggeut.domain.posting.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jjonggeut.domain.posting.entity.PostingReportType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 모집공고 신고 요청. 기타(ETC)는 어떤 문제인지 알 수 없으므로 내용이 필요하다. */
public record PostingReportRequest(

		@NotNull(message = "신고 유형을 선택해주세요.")
		PostingReportType reportType,

		@Size(max = 1000, message = "신고 내용은 1000자 이하로 입력해주세요.")
		String content) {

	@JsonIgnore
	@AssertTrue(message = "기타 신고는 내용을 입력해주세요.")
	public boolean isContentPresentForEtc() {
		return reportType != PostingReportType.ETC || content != null && !content.isBlank();
	}
}
