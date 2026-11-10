package com.jjonggeut.domain.shelter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjonggeut.domain.recommendation.entity.ShelterRef;
import com.jjonggeut.domain.recommendation.repository.ShelterRefRepository;
import com.jjonggeut.domain.shelter.entity.ShelterReport;
import com.jjonggeut.domain.shelter.entity.ShelterReportStatus;
import com.jjonggeut.domain.shelter.entity.ShelterReportType;
import com.jjonggeut.domain.shelter.repository.ShelterReportRepository;
import com.jjonggeut.domain.user.entity.User;
import com.jjonggeut.domain.user.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ShelterReportTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ShelterRefRepository shelterRepository;

	@Autowired
	private ShelterReportRepository reportRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private ObjectMapper objectMapper;

	private User user;
	private User other;
	private String token;

	@BeforeEach
	void setUp() throws Exception {
		user = userRepository.save(User.createUser("jjong123", passwordEncoder.encode("password1"), "김쫑긋", "쫑긋이"));
		other = userRepository.save(User.createUser("other456", passwordEncoder.encode("password1"), "이다른", "다른이"));
		String response = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"loginId":"jjong123","password":"password1"}
								"""))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		token = objectMapper.readTree(response).get("data").get("accessToken").asText();

		shelterRepository.save(ShelterRef.of(1L, "성북 행복보호소", "서울특별시", "성북구", 37.5894, 127.0167));
		shelterRepository.save(ShelterRef.of(2L, "노원 온기쉼터", "서울특별시", "노원구", 37.6542, 127.0568));
	}

	private ResultActions report(Object shelterId, String body) throws Exception {
		return mockMvc.perform(post("/api/v1/shelters/" + shelterId + "/reports")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}

	private String body(String type, String content) {
		return content == null
				? "{\"reportType\":\"" + type + "\"}"
				: "{\"reportType\":\"" + type + "\",\"content\":\"" + content + "\"}";
	}

	@Test
	void 신고를_접수한다() throws Exception {
		report(1, body("WRONG_INFO", "연락처가 실제와 다릅니다.")).andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.data.reportId").isNumber())
				.andExpect(jsonPath("$.data.shelterId").value(1))
				.andExpect(jsonPath("$.data.reportType").value("WRONG_INFO"))
				.andExpect(jsonPath("$.data.status").value("RECEIVED"))
				.andExpect(jsonPath("$.data.createdAt").isNotEmpty());

		List<ShelterReport> saved = reportRepository.findAll();
		assertEquals(1, saved.size());
		assertEquals(user.getUserId(), saved.get(0).getReporterUserId());
		assertEquals(1L, saved.get(0).getShelterId());
		assertEquals("연락처가 실제와 다릅니다.", saved.get(0).getContent());
		assertEquals(ShelterReportStatus.RECEIVED, saved.get(0).getStatus());
	}

	@Test
	void 내용은_선택이고_앞뒤_공백은_제거한다() throws Exception {
		report(1, body("FRAUD", null)).andExpect(status().isOk());
		report(1, body("PRIVACY", "   ")).andExpect(status().isOk());
		report(2, body("WRONG_INFO", "  주소 오류  ")).andExpect(status().isOk());

		List<ShelterReport> saved = reportRepository.findAll();
		assertEquals(3, saved.size());
		assertNull(saved.get(0).getContent());
		assertNull(saved.get(1).getContent());
		assertEquals("주소 오류", saved.get(2).getContent());
	}

	@Test
	void 기타_신고는_내용이_있어야_한다() throws Exception {
		report(1, body("ETC", null)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_2"));
		report(1, body("ETC", "  ")).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_2"));
		report(1, body("ETC", "그 밖의 문제입니다.")).andExpect(status().isOk());
	}

	@Test
	void 내용이_1000자를_넘으면_400() throws Exception {
		report(1, body("WRONG_INFO", "가".repeat(1000))).andExpect(status().isOk());
		report(2, body("WRONG_INFO", "가".repeat(1001))).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_2"));
	}

	@Test
	void 같은_유형의_처리_중_신고가_있으면_409() throws Exception {
		report(1, body("FRAUD", "사기가 의심됩니다.")).andExpect(status().isOk());

		report(1, body("FRAUD", "다시 신고합니다.")).andExpect(status().isConflict())
				.andExpect(jsonPath("$.isSuccess").value(false))
				.andExpect(jsonPath("$.code").value("SHELTER_409_2"));
		assertEquals(1, reportRepository.count());
	}

	@Test
	void 검토_중인_신고도_중복으로_본다() throws Exception {
		reportRepository.save(ShelterReport.of(1L, user.getUserId(), ShelterReportType.FRAUD, null,
				ShelterReportStatus.IN_REVIEW));

		report(1, body("FRAUD", null)).andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("SHELTER_409_2"));
	}

	@Test
	void 처리가_끝난_신고가_있으면_다시_신고할_수_있다() throws Exception {
		reportRepository.save(ShelterReport.of(1L, user.getUserId(), ShelterReportType.FRAUD, null,
				ShelterReportStatus.RESOLVED));
		reportRepository.save(ShelterReport.of(1L, user.getUserId(), ShelterReportType.PRIVACY, null,
				ShelterReportStatus.REJECTED));

		report(1, body("FRAUD", null)).andExpect(status().isOk());
		report(1, body("PRIVACY", null)).andExpect(status().isOk());
	}

	@Test
	void 유형이_다르거나_시설이_다르거나_신고자가_다르면_중복이_아니다() throws Exception {
		report(1, body("FRAUD", null)).andExpect(status().isOk());

		report(1, body("WRONG_INFO", null)).andExpect(status().isOk());
		report(2, body("FRAUD", null)).andExpect(status().isOk());

		reportRepository.save(ShelterReport.of(2L, other.getUserId(), ShelterReportType.WRONG_INFO, null,
				ShelterReportStatus.RECEIVED));
		report(2, body("WRONG_INFO", null)).andExpect(status().isOk());
		assertEquals(4, reportRepository.findAll().stream()
				.filter(r -> r.getReporterUserId().equals(user.getUserId())).count());
	}

	@Test
	void 없는_시설이면_404() throws Exception {
		report(9999, body("FRAUD", null)).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("SHELTER_404_1"));
	}

	@Test
	void 시설_ID가_1_미만이면_400이고_숫자가_아니면_COMMON_400_1() throws Exception {
		report(0, body("FRAUD", null)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_2"));
		report("abc", body("FRAUD", null)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_1"));
	}

	@Test
	void 신고_유형이_없거나_틀리면_400() throws Exception {
		report(1, "{}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
		report(1, "{\"content\":\"내용\"}").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_2"));
		report(1, body("SPAM", null)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_1"));
		report(1, body("fraud", null)).andExpect(status().isBadRequest());
		report(1, "not json").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_1"));
	}

	@Test
	void 토큰이_없으면_401() throws Exception {
		mockMvc.perform(post("/api/v1/shelters/1/reports")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body("FRAUD", null)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("COMMON_401_1"));
	}
}
