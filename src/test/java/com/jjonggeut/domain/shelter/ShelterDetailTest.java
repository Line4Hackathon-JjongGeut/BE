package com.jjonggeut.domain.shelter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjonggeut.domain.activity.entity.ShelterBookmarkRef;
import com.jjonggeut.domain.activity.repository.ShelterBookmarkRefRepository;
import com.jjonggeut.domain.profile.entity.InterestAnimal;
import com.jjonggeut.domain.recommendation.entity.PostingRef;
import com.jjonggeut.domain.recommendation.entity.ShelterRef;
import com.jjonggeut.domain.recommendation.repository.PostingRefRepository;
import com.jjonggeut.domain.recommendation.repository.ShelterRefRepository;
import com.jjonggeut.domain.shelter.entity.VerificationStatus;
import com.jjonggeut.domain.user.entity.User;
import com.jjonggeut.domain.user.repository.UserRepository;
import com.jjonggeut.domain.volunteerexperience.entity.ActivityCategory;
import java.time.LocalDateTime;
import java.util.List;
import org.hamcrest.Matchers;
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
class ShelterDetailTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ShelterRefRepository shelterRepository;

	@Autowired
	private PostingRefRepository postingRepository;

	@Autowired
	private ShelterBookmarkRefRepository bookmarkRepository;

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

		// 1: 인증 시설(성북), 2: 미인증 시설(노원)
		shelterRepository.save(ShelterRef.of(1L, "성북 행복보호소", "서울특별시", "성북구", "서울특별시 성북구 1번지",
				37.5894, 127.0167, "02-111-2222", "유기견·유기묘를 돌봅니다.",
				List.of(InterestAnimal.DOG, InterestAnimal.CAT), VerificationStatus.VERIFIED));
		shelterRepository.save(ShelterRef.of(2L, "노원 온기쉼터", "서울특별시", "노원구", "서울특별시 노원구 2번지",
				37.6542, 127.0568, "02-333-4444", "고양이 쉼터",
				List.of(InterestAnimal.CAT), VerificationStatus.UNVERIFIED));
	}

	private ResultActions detail(Object shelterId, String query) throws Exception {
		return mockMvc.perform(get("/api/v1/shelters/" + shelterId + (query.isEmpty() ? "" : "?" + query))
				.header("Authorization", "Bearer " + token));
	}

	private void posting(long id, long shelterId, String status, LocalDateTime startAt) {
		postingRepository.save(PostingRef.of(id, shelterId, "VOLUNTEER", "공고" + id, status,
				List.of(ActivityCategory.WALK), "DOG", startAt, 10, 3));
	}

	@Test
	void 인증_시설의_상세를_보여_준다() throws Exception {
		detail(1, "").andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.data.shelterId").value(1))
				.andExpect(jsonPath("$.data.name").value("성북 행복보호소"))
				.andExpect(jsonPath("$.data.region.sido").value("서울특별시"))
				.andExpect(jsonPath("$.data.region.sigungu").value("성북구"))
				.andExpect(jsonPath("$.data.address").value("서울특별시 성북구 1번지"))
				.andExpect(jsonPath("$.data.latitude").value(37.5894))
				.andExpect(jsonPath("$.data.longitude").value(127.0167))
				.andExpect(jsonPath("$.data.phone").value("02-111-2222"))
				.andExpect(jsonPath("$.data.description").value("유기견·유기묘를 돌봅니다."))
				.andExpect(jsonPath("$.data.animalTypes[0]").value("DOG"))
				.andExpect(jsonPath("$.data.animalTypes[1]").value("CAT"))
				.andExpect(jsonPath("$.data.verificationStatus").value("VERIFIED"))
				.andExpect(jsonPath("$.data.distanceKm").isEmpty())
				.andExpect(jsonPath("$.data.bookmarked").value(false))
				.andExpect(jsonPath("$.data.openPostingCount").value(0))
				.andExpect(jsonPath("$.data.openPostings").isEmpty());
	}

	@Test
	void 미인증_시설은_연락처를_가린다() throws Exception {
		detail(2, "").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.verificationStatus").value("UNVERIFIED"))
				.andExpect(jsonPath("$.data.phone").isEmpty())
				.andExpect(jsonPath("$.data.name").value("노원 온기쉼터"));
	}

	@Test
	void 값이_없는_시설은_미인증이고_빈_값으로_내려간다() throws Exception {
		shelterRepository.save(ShelterRef.of(3L, "정보없는 시설", null, null, null, null));

		detail(3, "").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.region").isEmpty())
				.andExpect(jsonPath("$.data.phone").isEmpty())
				.andExpect(jsonPath("$.data.animalTypes.length()").value(0))
				.andExpect(jsonPath("$.data.verificationStatus").value("UNVERIFIED"));
	}

	@Test
	void 좌표를_보내면_거리를_함께_준다() throws Exception {
		detail(2, "latitude=37.5894&longitude=127.0167").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.distanceKm").value(Matchers.closeTo(8.0, 1.0)));
		detail(1, "latitude=37.5894&longitude=127.0167").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.distanceKm").value(0.0));
	}

	@Test
	void 시설_좌표가_없으면_거리는_null이다() throws Exception {
		shelterRepository.save(ShelterRef.of(3L, "좌표없는 시설", "서울특별시", "중구", null, null));

		detail(3, "latitude=37.5894&longitude=127.0167").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.distanceKm").isEmpty());
	}

	@Test
	void 모집_중_공고만_활동일_빠른_순으로_보여_주고_일시_없는_공고는_뒤에_온다() throws Exception {
		posting(101, 1, "OPEN", LocalDateTime.of(2026, 12, 20, 10, 0));
		posting(102, 1, "OPEN", null);
		posting(103, 1, "OPEN", LocalDateTime.of(2026, 12, 5, 10, 0));
		posting(104, 1, "CLOSED", LocalDateTime.of(2026, 12, 1, 10, 0));
		posting(105, 2, "OPEN", LocalDateTime.of(2026, 12, 1, 10, 0));

		detail(1, "").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.openPostingCount").value(3))
				.andExpect(jsonPath("$.data.openPostings.length()").value(3))
				.andExpect(jsonPath("$.data.openPostings[0].postingId").value(103))
				.andExpect(jsonPath("$.data.openPostings[0].postingType").value("VOLUNTEER"))
				.andExpect(jsonPath("$.data.openPostings[0].title").value("공고103"))
				.andExpect(jsonPath("$.data.openPostings[0].activityStartAt").value("2026-12-05T10:00:00"))
				.andExpect(jsonPath("$.data.openPostings[0].capacity").value(10))
				.andExpect(jsonPath("$.data.openPostings[0].applicantCount").value(3))
				.andExpect(jsonPath("$.data.openPostings[1].postingId").value(101))
				.andExpect(jsonPath("$.data.openPostings[2].postingId").value(102))
				.andExpect(jsonPath("$.data.openPostings[2].activityStartAt").isEmpty());
	}

	@Test
	void 공고는_최대_10건만_주고_개수는_전체를_센다() throws Exception {
		for (int i = 0; i < 12; i++) {
			posting(200 + i, 1, "OPEN", LocalDateTime.of(2026, 12, 1, 10, 0).plusDays(i));
		}

		detail(1, "").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.openPostingCount").value(12))
				.andExpect(jsonPath("$.data.openPostings.length()").value(10))
				.andExpect(jsonPath("$.data.openPostings[0].postingId").value(200));
	}

	@Test
	void 내_관심_저장_여부만_반영한다() throws Exception {
		bookmarkRepository.save(ShelterBookmarkRef.of(user.getUserId(), 1L, LocalDateTime.of(2026, 11, 1, 9, 0)));
		bookmarkRepository.save(ShelterBookmarkRef.of(other.getUserId(), 2L, LocalDateTime.of(2026, 11, 1, 9, 0)));

		detail(1, "").andExpect(status().isOk()).andExpect(jsonPath("$.data.bookmarked").value(true));
		detail(2, "").andExpect(status().isOk()).andExpect(jsonPath("$.data.bookmarked").value(false));
	}

	@Test
	void 없는_시설이면_404() throws Exception {
		detail(9999, "").andExpect(status().isNotFound())
				.andExpect(jsonPath("$.isSuccess").value(false))
				.andExpect(jsonPath("$.code").value("SHELTER_404_1"));
	}

	@Test
	void 시설_ID가_1_미만이면_400이고_숫자가_아니면_COMMON_400_1() throws Exception {
		detail(0, "").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
		detail(-1, "").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
		detail("abc", "").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_1"));
	}

	@Test
	void 좌표는_함께_보내야_하고_범위_안이어야_한다() throws Exception {
		detail(1, "latitude=37.5").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_2"));
		detail(1, "longitude=127.0").andExpect(status().isBadRequest());
		detail(1, "latitude=91&longitude=127").andExpect(status().isBadRequest());
		detail(1, "latitude=37&longitude=181").andExpect(status().isBadRequest());
		detail(1, "latitude=abc&longitude=127").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_1"));
	}

	@Test
	void 토큰이_없으면_401() throws Exception {
		mockMvc.perform(get("/api/v1/shelters/1"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("COMMON_401_1"));
	}
}
