package com.jjonggeut.domain.shelter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjonggeut.domain.activity.entity.PostingBookmarkRef;
import com.jjonggeut.domain.activity.repository.PostingBookmarkRefRepository;
import com.jjonggeut.domain.recommendation.entity.PostingRef;
import com.jjonggeut.domain.recommendation.entity.ShelterRef;
import com.jjonggeut.domain.recommendation.repository.PostingRefRepository;
import com.jjonggeut.domain.recommendation.repository.ShelterRefRepository;
import com.jjonggeut.domain.user.entity.User;
import com.jjonggeut.domain.user.repository.UserRepository;
import com.jjonggeut.domain.volunteerexperience.entity.ActivityCategory;
import java.time.LocalDateTime;
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
class ShelterPostingListTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ShelterRefRepository shelterRepository;

	@Autowired
	private PostingRefRepository postingRepository;

	@Autowired
	private PostingBookmarkRefRepository bookmarkRepository;

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

	private ResultActions list(Object shelterId, String query) throws Exception {
		return mockMvc.perform(get("/api/v1/shelters/" + shelterId + "/postings" + (query.isEmpty() ? "" : "?" + query))
				.header("Authorization", "Bearer " + token));
	}

	private void posting(long id, long shelterId, String type, String status, LocalDateTime startAt, Integer capacity,
			Integer applicants) {
		postingRepository.save(PostingRef.of(id, shelterId, type, "공고" + id, status,
				List.of(ActivityCategory.WALK, ActivityCategory.CARE), "DOG", startAt, capacity, applicants));
	}

	@Test
	void 모집_중_공고만_활동일_빠른_순으로_보여_준다() throws Exception {
		posting(101, 1, "VOLUNTEER", "OPEN", LocalDateTime.of(2026, 12, 20, 10, 0), 10, 3);
		posting(102, 1, "SPONSOR", "OPEN", null, null, null);
		posting(103, 1, "FOSTER", "OPEN", LocalDateTime.of(2026, 12, 5, 10, 0), 2, 2);
		posting(104, 1, "VOLUNTEER", "CLOSED", LocalDateTime.of(2026, 12, 1, 10, 0), 10, 10);
		posting(105, 2, "VOLUNTEER", "OPEN", LocalDateTime.of(2026, 12, 1, 10, 0), 10, 0);

		list(1, "").andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.data.items.length()").value(3))
				.andExpect(jsonPath("$.data.items[0].postingId").value(103))
				.andExpect(jsonPath("$.data.items[1].postingId").value(101))
				.andExpect(jsonPath("$.data.items[2].postingId").value(102))
				.andExpect(jsonPath("$.data.page").value(0))
				.andExpect(jsonPath("$.data.size").value(20))
				.andExpect(jsonPath("$.data.totalElements").value(3))
				.andExpect(jsonPath("$.data.totalPages").value(1))
				.andExpect(jsonPath("$.data.hasNext").value(false));
	}

	@Test
	void 항목에_공고_정보와_남은_자리가_담긴다() throws Exception {
		posting(101, 1, "VOLUNTEER", "OPEN", LocalDateTime.of(2026, 12, 20, 10, 0), 10, 3);

		list(1, "").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items[0].postingId").value(101))
				.andExpect(jsonPath("$.data.items[0].postingType").value("VOLUNTEER"))
				.andExpect(jsonPath("$.data.items[0].title").value("공고101"))
				.andExpect(jsonPath("$.data.items[0].activityCategories[0]").value("WALK"))
				.andExpect(jsonPath("$.data.items[0].activityCategories[1]").value("CARE"))
				.andExpect(jsonPath("$.data.items[0].animalType").value("DOG"))
				.andExpect(jsonPath("$.data.items[0].activityStartAt").value("2026-12-20T10:00:00"))
				.andExpect(jsonPath("$.data.items[0].capacity").value(10))
				.andExpect(jsonPath("$.data.items[0].applicantCount").value(3))
				.andExpect(jsonPath("$.data.items[0].remainingCount").value(7))
				.andExpect(jsonPath("$.data.items[0].bookmarked").value(false));
	}

	@Test
	void 모집_인원이_없으면_남은_자리는_null이고_초과_신청은_0이다() throws Exception {
		posting(101, 1, "SPONSOR", "OPEN", null, null, null);
		posting(102, 1, "VOLUNTEER", "OPEN", LocalDateTime.of(2026, 12, 1, 10, 0), 5, 8);
		posting(103, 1, "VOLUNTEER", "OPEN", LocalDateTime.of(2026, 12, 2, 10, 0), 5, null);

		list(1, "").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items[?(@.postingId==101)].remainingCount").value(
						org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.nullValue())))
				.andExpect(jsonPath("$.data.items[?(@.postingId==102)].remainingCount").value(0))
				.andExpect(jsonPath("$.data.items[?(@.postingId==103)].remainingCount").value(5));
	}

	@Test
	void 공고_종류로_거른다() throws Exception {
		posting(101, 1, "VOLUNTEER", "OPEN", LocalDateTime.of(2026, 12, 20, 10, 0), 10, 3);
		posting(102, 1, "SPONSOR", "OPEN", null, null, null);
		posting(103, 1, "FOSTER", "OPEN", LocalDateTime.of(2026, 12, 5, 10, 0), 2, 1);
		posting(104, 1, "VOLUNTEER", "OPEN", LocalDateTime.of(2026, 12, 10, 10, 0), 10, 3);

		list(1, "postingType=VOLUNTEER").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(2))
				.andExpect(jsonPath("$.data.items[0].postingId").value(104))
				.andExpect(jsonPath("$.data.items[1].postingId").value(101));
		list(1, "postingType=SPONSOR").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(1))
				.andExpect(jsonPath("$.data.items[0].postingId").value(102));
		list(1, "postingType=FOSTER").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items[0].postingId").value(103));
	}

	@Test
	void 공고가_없으면_빈_목록이다() throws Exception {
		list(2, "").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items").isEmpty())
				.andExpect(jsonPath("$.data.totalElements").value(0));
	}

	@Test
	void 페이지로_나눠_준다() throws Exception {
		for (int i = 0; i < 5; i++) {
			posting(200 + i, 1, "VOLUNTEER", "OPEN", LocalDateTime.of(2026, 12, 1, 10, 0).plusDays(i), 10, 0);
		}

		list(1, "page=0&size=2").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items.length()").value(2))
				.andExpect(jsonPath("$.data.items[0].postingId").value(200))
				.andExpect(jsonPath("$.data.totalElements").value(5))
				.andExpect(jsonPath("$.data.totalPages").value(3))
				.andExpect(jsonPath("$.data.hasNext").value(true));
		list(1, "page=2&size=2").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items.length()").value(1))
				.andExpect(jsonPath("$.data.items[0].postingId").value(204))
				.andExpect(jsonPath("$.data.hasNext").value(false));
	}

	@Test
	void 내_관심_공고만_bookmarked로_표시한다() throws Exception {
		posting(101, 1, "VOLUNTEER", "OPEN", LocalDateTime.of(2026, 12, 1, 10, 0), 10, 0);
		posting(102, 1, "VOLUNTEER", "OPEN", LocalDateTime.of(2026, 12, 2, 10, 0), 10, 0);
		bookmarkRepository.save(PostingBookmarkRef.of(user.getUserId(), 101L, LocalDateTime.of(2026, 11, 1, 9, 0)));
		bookmarkRepository.save(PostingBookmarkRef.of(other.getUserId(), 102L, LocalDateTime.of(2026, 11, 1, 9, 0)));

		list(1, "").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items[0].postingId").value(101))
				.andExpect(jsonPath("$.data.items[0].bookmarked").value(true))
				.andExpect(jsonPath("$.data.items[1].postingId").value(102))
				.andExpect(jsonPath("$.data.items[1].bookmarked").value(false));
	}

	@Test
	void 없는_시설이면_404() throws Exception {
		list(9999, "").andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("SHELTER_404_1"));
	}

	@Test
	void 시설_ID와_공고_종류_page_size가_틀리면_400() throws Exception {
		list(0, "").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
		list(1, "postingType=DONATION").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_2"));
		list(1, "postingType=volunteer").andExpect(status().isBadRequest());
		list(1, "page=-1").andExpect(status().isBadRequest());
		list(1, "size=0").andExpect(status().isBadRequest());
		list(1, "size=51").andExpect(status().isBadRequest());
	}

	@Test
	void 숫자가_아니면_COMMON_400_1() throws Exception {
		list("abc", "").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_1"));
		list(1, "page=abc").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_1"));
	}

	@Test
	void 토큰이_없으면_401() throws Exception {
		mockMvc.perform(get("/api/v1/shelters/1/postings"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("COMMON_401_1"));
	}
}
