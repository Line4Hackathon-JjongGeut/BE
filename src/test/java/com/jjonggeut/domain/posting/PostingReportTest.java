package com.jjonggeut.domain.posting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jjonggeut.domain.posting.entity.PostingReport;
import com.jjonggeut.domain.posting.entity.PostingReportStatus;
import com.jjonggeut.domain.posting.entity.PostingReportType;
import com.jjonggeut.domain.posting.repository.PostingReportRepository;
import org.springframework.test.web.servlet.ResultActions;
import com.jjonggeut.domain.shelter.entity.VerificationStatus;
import com.jjonggeut.domain.volunteerexperience.entity.ActivityCategory;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PostingReportTest extends PostingTestBase {

	@Autowired
	PostingReportRepository reportRepository;

	@BeforeEach
	void setUpData() {
		shelter(1, "성북 행복보호소", "서울특별시", "성북구", 37.5894, 127.0167, VerificationStatus.VERIFIED);
		posting(301, 1, "VOLUNTEER", "OPEN", "산책 봉사", List.of(ActivityCategory.WALK), "DOG", null, 5, 0);
		posting(302, 1, "VOLUNTEER", "OPEN", "비공개 공고", List.of(ActivityCategory.WALK), "DOG", null, 5, 0)
				.withPublic(false);
		posting(303, 1, "VOLUNTEER", "CLOSED", "마감 공고", List.of(ActivityCategory.WALK), "DOG", null, 5, 5);
		savePostings();
	}

	private ResultActions report(Object postingId, String body) throws Exception {
		return send(post("/api/v1/postings/" + postingId + "/reports"), body);
	}

	private String body(String type, String content) {
		return content == null
				? "{\"reportType\":\"" + type + "\"}"
				: "{\"reportType\":\"" + type + "\",\"content\":\"" + content + "\"}";
	}

	@Test
	void 신고를_접수한다() throws Exception {
		report(301, body("FALSE_INFO", "활동 장소가 다릅니다.")).andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.data.reportId").isNumber())
				.andExpect(jsonPath("$.data.postingId").value(301))
				.andExpect(jsonPath("$.data.reportType").value("FALSE_INFO"))
				.andExpect(jsonPath("$.data.status").value("RECEIVED"))
				.andExpect(jsonPath("$.data.createdAt").isNotEmpty());

		var saved = reportRepository.findAll();
		assertThat(saved).hasSize(1);
		assertThat(saved.get(0).getReporterUserId()).isEqualTo(user.getUserId());
		assertThat(saved.get(0).getPostingId()).isEqualTo(301L);
		assertThat(saved.get(0).getContent()).isEqualTo("활동 장소가 다릅니다.");
		assertThat(saved.get(0).getStatus()).isEqualTo(PostingReportStatus.RECEIVED);
	}

	@Test
	void 내용은_선택이고_앞뒤_공백은_제거한다() throws Exception {
		report(301, body("INAPPROPRIATE", null)).andExpect(status().isOk());
		report(301, body("OUTDATED", "   ")).andExpect(status().isOk());
		report(303, body("FALSE_INFO", "  지난 공고  ")).andExpect(status().isOk());

		var saved = reportRepository.findAll();
		assertThat(saved).hasSize(3);
		assertThat(saved.get(0).getContent()).isNull();
		assertThat(saved.get(1).getContent()).isNull();
		assertThat(saved.get(2).getContent()).isEqualTo("지난 공고");
	}

	@Test
	void 기타_신고는_내용이_있어야_한다() throws Exception {
		report(301, body("ETC", null)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_2"));
		report(301, body("ETC", "  ")).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_2"));
		report(301, body("ETC", "그 밖의 문제입니다.")).andExpect(status().isOk());
	}

	@Test
	void 내용이_1000자를_넘으면_400() throws Exception {
		report(301, body("FALSE_INFO", "가".repeat(1000))).andExpect(status().isOk());
		report(303, body("FALSE_INFO", "가".repeat(1001))).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_2"));
	}

	@Test
	void 같은_유형의_처리_중_신고가_있으면_409() throws Exception {
		report(301, body("FALSE_INFO", "허위입니다.")).andExpect(status().isOk());

		report(301, body("FALSE_INFO", "다시 신고합니다.")).andExpect(status().isConflict())
				.andExpect(jsonPath("$.isSuccess").value(false))
				.andExpect(jsonPath("$.code").value("POSTING_409_2"));
		assertThat(reportRepository.count()).isEqualTo(1);
	}

	@Test
	void 검토_중인_신고도_중복으로_본다() throws Exception {
		reportRepository.save(PostingReport.of(301L, user.getUserId(), PostingReportType.FALSE_INFO, null,
				PostingReportStatus.IN_REVIEW));

		report(301, body("FALSE_INFO", null)).andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("POSTING_409_2"));
	}

	@Test
	void 처리가_끝난_신고가_있으면_다시_신고할_수_있다() throws Exception {
		reportRepository.save(PostingReport.of(301L, user.getUserId(), PostingReportType.FALSE_INFO, null,
				PostingReportStatus.RESOLVED));
		reportRepository.save(PostingReport.of(301L, user.getUserId(), PostingReportType.OUTDATED, null,
				PostingReportStatus.REJECTED));

		report(301, body("FALSE_INFO", null)).andExpect(status().isOk());
		report(301, body("OUTDATED", null)).andExpect(status().isOk());
	}

	@Test
	void 유형이_다르거나_공고가_다르거나_신고자가_다르면_중복이_아니다() throws Exception {
		report(301, body("FALSE_INFO", null)).andExpect(status().isOk());

		report(301, body("OUTDATED", null)).andExpect(status().isOk());
		report(303, body("FALSE_INFO", null)).andExpect(status().isOk());

		reportRepository.save(PostingReport.of(303L, other.getUserId(), PostingReportType.OUTDATED, null,
				PostingReportStatus.RECEIVED));
		report(303, body("OUTDATED", null)).andExpect(status().isOk());
		assertThat(reportRepository.findAll().stream()
				.filter(r -> r.getReporterUserId().equals(user.getUserId())).count()).isEqualTo(4);
	}

	@Test
	void 없거나_비공개인_공고는_404() throws Exception {
		report(9999, body("FALSE_INFO", null)).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("POSTING_404_1"));
		report(302, body("FALSE_INFO", null)).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("POSTING_404_1"));
	}

	@Test
	void 공고_ID가_1_미만이면_400이고_숫자가_아니면_COMMON_400_1() throws Exception {
		report(0, body("FALSE_INFO", null)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_2"));
		report("abc", body("FALSE_INFO", null)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_1"));
	}

	@Test
	void 신고_유형이_없거나_틀리면_400() throws Exception {
		report(301, "{}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
		report(301, "{\"content\":\"내용\"}").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_2"));
		report(301, body("SPAM", null)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_1"));
	}

	@Test
	void 본문이_없거나_깨졌으면_400() throws Exception {
		send(post("/api/v1/postings/301/reports"), null).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_1"));
		report(301, "{broken").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_1"));
	}

	@Test
	void 토큰이_없으면_401() throws Exception {
		mockMvc.perform(post("/api/v1/postings/301/reports")
						.contentType(org.springframework.http.MediaType.APPLICATION_JSON)
						.content(body("FALSE_INFO", null)))
				.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("COMMON_401_1"));
	}
}
