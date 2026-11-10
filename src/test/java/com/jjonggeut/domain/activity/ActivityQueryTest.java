package com.jjonggeut.domain.activity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjonggeut.domain.activity.entity.ApplicationStatus;
import com.jjonggeut.domain.activity.entity.NotificationRef;
import com.jjonggeut.domain.activity.entity.PostingBookmarkRef;
import com.jjonggeut.domain.activity.entity.ShelterBookmarkRef;
import com.jjonggeut.domain.activity.entity.VolunteerApplicationRef;
import com.jjonggeut.domain.activity.repository.NotificationRefRepository;
import com.jjonggeut.domain.activity.repository.PostingBookmarkRefRepository;
import com.jjonggeut.domain.activity.repository.ShelterBookmarkRefRepository;
import com.jjonggeut.domain.activity.repository.VolunteerApplicationRefRepository;
import com.jjonggeut.domain.recommendation.entity.PostingRef;
import com.jjonggeut.domain.recommendation.entity.ShelterRef;
import com.jjonggeut.domain.recommendation.repository.PostingRefRepository;
import com.jjonggeut.domain.recommendation.repository.ShelterRefRepository;
import com.jjonggeut.domain.user.entity.User;
import com.jjonggeut.domain.user.repository.UserRepository;
import com.jjonggeut.domain.volunteerexperience.entity.ActivityCategory;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
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
class ActivityQueryTest {

	private static final String BASE = "/api/v1/users/me";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PostingRefRepository postingRepository;

	@Autowired
	private ShelterRefRepository shelterRepository;

	@Autowired
	private PostingBookmarkRefRepository postingBookmarkRepository;

	@Autowired
	private ShelterBookmarkRefRepository shelterBookmarkRepository;

	@Autowired
	private VolunteerApplicationRefRepository applicationRepository;

	@Autowired
	private NotificationRefRepository notificationRepository;

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
		token = login("jjong123");

		shelterRepository.save(ShelterRef.of(1L, "성북 행복보호소", "서울특별시", "성북구", 37.5894, 127.0167));
		shelterRepository.save(ShelterRef.of(2L, "노원 온기쉼터", "서울특별시", "노원구", 37.6542, 127.0568));
	}

	private String login(String loginId) throws Exception {
		String response = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"loginId":"%s","password":"password1"}
								""".formatted(loginId)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(response).get("data").get("accessToken").asText();
	}

	private ResultActions call(String url) throws Exception {
		return mockMvc.perform(get(BASE + url).header("Authorization", "Bearer " + token));
	}

	private PostingRef posting(long id, long shelterId, String status, LocalDateTime start) {
		return postingRepository.save(PostingRef.of(id, shelterId, "VOLUNTEER", "공고" + id, status,
				List.of(ActivityCategory.WALK), "DOG", start, 10, 3));
	}

	private void apply(User who, long postingId, ApplicationStatus status) {
		applicationRepository.save(VolunteerApplicationRef.of(who.getUserId(), postingId, status,
				LocalDateTime.of(2026, 10, 1, 9, 0)));
	}

	private static LocalDateTime at(int month, int day, int hour) {
		return LocalDateTime.of(2026, month, day, hour, 0);
	}

	// ================= 내 활동 요약 =================

	@Test
	void 활동_요약은_기록이_없으면_모두_0() throws Exception {
		call("/activity-summary").andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.data.bookmarkedPostingCount").value(0))
				.andExpect(jsonPath("$.data.ongoingApplicationCount").value(0))
				.andExpect(jsonPath("$.data.completedParticipationCount").value(0))
				.andExpect(jsonPath("$.data.unreadNotificationCount").value(0));
	}

	@Test
	void 활동_요약은_내_기록만_상태별로_센다() throws Exception {
		posting(101, 1, "OPEN", at(12, 1, 10));
		posting(102, 1, "OPEN", at(12, 2, 10));
		posting(103, 2, "CLOSED", at(12, 3, 10));
		postingBookmarkRepository.save(PostingBookmarkRef.of(user.getUserId(), 101L, at(11, 1, 9)));
		postingBookmarkRepository.save(PostingBookmarkRef.of(user.getUserId(), 102L, at(11, 2, 9)));
		postingBookmarkRepository.save(PostingBookmarkRef.of(other.getUserId(), 101L, at(11, 2, 9)));

		apply(user, 101, ApplicationStatus.PENDING);
		apply(user, 102, ApplicationStatus.CONFIRMED);
		apply(user, 103, ApplicationStatus.COMPLETED);
		apply(user, 101, ApplicationStatus.COMPLETED);
		apply(user, 102, ApplicationStatus.CANCELLED);
		apply(user, 103, ApplicationStatus.REJECTED);
		apply(user, 103, ApplicationStatus.NO_SHOW);
		apply(other, 101, ApplicationStatus.PENDING);
		apply(other, 101, ApplicationStatus.COMPLETED);

		notificationRepository.save(NotificationRef.of(user.getUserId(), null, at(11, 1, 9)));
		notificationRepository.save(NotificationRef.of(user.getUserId(), null, at(11, 2, 9)));
		notificationRepository.save(NotificationRef.of(user.getUserId(), at(11, 3, 9), at(11, 1, 9)));
		notificationRepository.save(NotificationRef.of(other.getUserId(), null, at(11, 1, 9)));

		call("/activity-summary").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.bookmarkedPostingCount").value(2))
				.andExpect(jsonPath("$.data.ongoingApplicationCount").value(2))
				.andExpect(jsonPath("$.data.completedParticipationCount").value(2))
				.andExpect(jsonPath("$.data.unreadNotificationCount").value(2));
	}

	@Test
	void 활동_요약은_토큰이_없으면_401() throws Exception {
		mockMvc.perform(get(BASE + "/activity-summary"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("COMMON_401_1"));
	}

	// ================= 내 활동 캘린더 =================

	@Test
	void 캘린더는_해당_월의_확정과_완료만_시작_순으로_보여_준다() throws Exception {
		posting(101, 1, "OPEN", at(11, 20, 14));
		posting(102, 2, "OPEN", at(11, 5, 10));
		posting(103, 1, "OPEN", at(11, 12, 9));
		apply(user, 101, ApplicationStatus.CONFIRMED);
		apply(user, 102, ApplicationStatus.COMPLETED);
		apply(user, 103, ApplicationStatus.PENDING);

		call("/activity-calendar?month=2026-11").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.month").value("2026-11"))
				.andExpect(jsonPath("$.data.items.length()").value(2))
				.andExpect(jsonPath("$.data.items[0].postingId").value(102))
				.andExpect(jsonPath("$.data.items[0].status").value("COMPLETED"))
				.andExpect(jsonPath("$.data.items[0].postingTitle").value("공고102"))
				.andExpect(jsonPath("$.data.items[0].shelterName").value("노원 온기쉼터"))
				.andExpect(jsonPath("$.data.items[0].activityStartAt").value("2026-11-05T10:00:00"))
				.andExpect(jsonPath("$.data.items[0].applicationId").isNotEmpty())
				.andExpect(jsonPath("$.data.items[1].postingId").value(101))
				.andExpect(jsonPath("$.data.items[1].status").value("CONFIRMED"));
	}

	@Test
	void 캘린더는_월_경계를_정확히_지킨다() throws Exception {
		posting(101, 1, "OPEN", LocalDateTime.of(2026, 10, 31, 23, 59));
		posting(102, 1, "OPEN", LocalDateTime.of(2026, 11, 1, 0, 0));
		posting(103, 1, "OPEN", LocalDateTime.of(2026, 11, 30, 23, 59));
		posting(104, 1, "OPEN", LocalDateTime.of(2026, 12, 1, 0, 0));
		for (long id = 101; id <= 104; id++) {
			apply(user, id, ApplicationStatus.CONFIRMED);
		}

		call("/activity-calendar?month=2026-11").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items.length()").value(2))
				.andExpect(jsonPath("$.data.items[0].postingId").value(102))
				.andExpect(jsonPath("$.data.items[1].postingId").value(103));
	}

	@Test
	void 캘린더는_다른_사용자와_끝난_신청은_제외한다() throws Exception {
		posting(101, 1, "OPEN", at(11, 10, 10));
		apply(other, 101, ApplicationStatus.CONFIRMED);
		apply(user, 101, ApplicationStatus.CANCELLED);
		apply(user, 101, ApplicationStatus.REJECTED);
		apply(user, 101, ApplicationStatus.NO_SHOW);

		call("/activity-calendar?month=2026-11").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items").isEmpty());
	}

	@Test
	void 캘린더는_month를_생략하면_이번_달이다() throws Exception {
		String thisMonth = YearMonth.now(ZoneId.of("Asia/Seoul")).toString();

		call("/activity-calendar").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.month").value(thisMonth))
				.andExpect(jsonPath("$.data.items").isEmpty());
	}

	@Test
	void 캘린더는_month_형식이_틀리면_400() throws Exception {
		for (String month : List.of("2026-13", "2026-00", "2026-1", "202611", "abc", "2026-11-01")) {
			call("/activity-calendar?month=" + month)
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.code").value("COMMON_400_2"));
		}
	}

	@Test
	void 캘린더는_토큰이_없으면_401() throws Exception {
		mockMvc.perform(get(BASE + "/activity-calendar"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("COMMON_401_1"));
	}

	// ================= 내 참여 이력 =================

	@Test
	void 참여_이력은_끝난_신청만_활동_시작_최신순으로_보여_준다() throws Exception {
		posting(101, 1, "CLOSED", at(9, 10, 10));
		posting(102, 2, "CLOSED", at(10, 10, 10));
		posting(103, 1, "CLOSED", at(8, 10, 10));
		posting(104, 1, "OPEN", at(12, 10, 10));
		posting(105, 1, "OPEN", at(12, 11, 10));
		apply(user, 101, ApplicationStatus.COMPLETED);
		apply(user, 102, ApplicationStatus.CANCELLED);
		apply(user, 103, ApplicationStatus.NO_SHOW);
		apply(user, 104, ApplicationStatus.PENDING);
		apply(user, 105, ApplicationStatus.CONFIRMED);
		apply(other, 101, ApplicationStatus.COMPLETED);

		call("/participation-history").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items.length()").value(3))
				.andExpect(jsonPath("$.data.items[0].postingId").value(102))
				.andExpect(jsonPath("$.data.items[0].status").value("CANCELLED"))
				.andExpect(jsonPath("$.data.items[0].shelterName").value("노원 온기쉼터"))
				.andExpect(jsonPath("$.data.items[0].activityStartAt").value("2026-10-10T10:00:00"))
				.andExpect(jsonPath("$.data.items[0].appliedAt").value("2026-10-01T09:00:00"))
				.andExpect(jsonPath("$.data.items[1].postingId").value(101))
				.andExpect(jsonPath("$.data.items[2].postingId").value(103))
				.andExpect(jsonPath("$.data.page").value(0))
				.andExpect(jsonPath("$.data.size").value(20))
				.andExpect(jsonPath("$.data.totalElements").value(3))
				.andExpect(jsonPath("$.data.totalPages").value(1))
				.andExpect(jsonPath("$.data.hasNext").value(false));
	}

	@Test
	void 참여_이력은_상태로_거를_수_있다() throws Exception {
		posting(101, 1, "CLOSED", at(9, 10, 10));
		posting(102, 1, "CLOSED", at(10, 10, 10));
		apply(user, 101, ApplicationStatus.COMPLETED);
		apply(user, 102, ApplicationStatus.REJECTED);

		call("/participation-history?status=REJECTED").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items.length()").value(1))
				.andExpect(jsonPath("$.data.items[0].status").value("REJECTED"))
				.andExpect(jsonPath("$.data.totalElements").value(1));
	}

	@Test
	void 참여_이력은_진행_중_상태나_알_수_없는_상태면_400() throws Exception {
		for (String status : List.of("PENDING", "CONFIRMED", "FOO", "completed")) {
			call("/participation-history?status=" + status)
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.code").value("COMMON_400_2"));
		}
	}

	@Test
	void 참여_이력은_페이지로_나눠_준다() throws Exception {
		for (long id = 101; id <= 105; id++) {
			posting(id, 1, "CLOSED", at(1, (int) (id - 100), 10));
			apply(user, id, ApplicationStatus.COMPLETED);
		}

		call("/participation-history?page=0&size=2").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items.length()").value(2))
				.andExpect(jsonPath("$.data.items[0].postingId").value(105))
				.andExpect(jsonPath("$.data.totalElements").value(5))
				.andExpect(jsonPath("$.data.totalPages").value(3))
				.andExpect(jsonPath("$.data.hasNext").value(true));
		call("/participation-history?page=2&size=2").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items.length()").value(1))
				.andExpect(jsonPath("$.data.items[0].postingId").value(101))
				.andExpect(jsonPath("$.data.hasNext").value(false));
		call("/participation-history?page=9&size=2").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items").isEmpty());
	}

	@Test
	void 목록은_page와_size_범위가_틀리면_400이고_숫자가_아니면_COMMON_400_1() throws Exception {
		for (String query : List.of("page=-1", "size=0", "size=51")) {
			call("/participation-history?" + query)
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.code").value("COMMON_400_2"));
			call("/bookmarked-postings?" + query).andExpect(status().isBadRequest());
			call("/bookmarked-shelters?" + query).andExpect(status().isBadRequest());
		}
		call("/participation-history?page=abc")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_1"));
	}

	@Test
	void 참여_이력은_토큰이_없으면_401() throws Exception {
		mockMvc.perform(get(BASE + "/participation-history"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("COMMON_401_1"));
	}

	// ================= 내 관심 공고 =================

	@Test
	void 관심_공고는_저장_최신순으로_모집_상태와_함께_보여_준다() throws Exception {
		posting(101, 1, "OPEN", at(12, 1, 10));
		posting(102, 2, "CLOSED", at(11, 1, 10));
		postingBookmarkRepository.save(PostingBookmarkRef.of(user.getUserId(), 101L, at(11, 1, 9)));
		postingBookmarkRepository.save(PostingBookmarkRef.of(user.getUserId(), 102L, at(11, 5, 9)));
		postingBookmarkRepository.save(PostingBookmarkRef.of(other.getUserId(), 101L, at(11, 6, 9)));

		call("/bookmarked-postings").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items.length()").value(2))
				.andExpect(jsonPath("$.data.items[0].postingId").value(102))
				.andExpect(jsonPath("$.data.items[0].status").value("CLOSED"))
				.andExpect(jsonPath("$.data.items[0].shelterName").value("노원 온기쉼터"))
				.andExpect(jsonPath("$.data.items[0].region.sido").value("서울특별시"))
				.andExpect(jsonPath("$.data.items[0].region.sigungu").value("노원구"))
				.andExpect(jsonPath("$.data.items[0].bookmarkedAt").value("2026-11-05T09:00:00"))
				.andExpect(jsonPath("$.data.items[1].postingId").value(101))
				.andExpect(jsonPath("$.data.items[1].postingType").value("VOLUNTEER"))
				.andExpect(jsonPath("$.data.items[1].title").value("공고101"))
				.andExpect(jsonPath("$.data.items[1].status").value("OPEN"))
				.andExpect(jsonPath("$.data.items[1].activityCategories[0]").value("WALK"))
				.andExpect(jsonPath("$.data.items[1].animalType").value("DOG"))
				.andExpect(jsonPath("$.data.items[1].activityStartAt").value("2026-12-01T10:00:00"))
				.andExpect(jsonPath("$.data.items[1].capacity").value(10))
				.andExpect(jsonPath("$.data.items[1].applicantCount").value(3))
				.andExpect(jsonPath("$.data.totalElements").value(2));
	}

	@Test
	void 관심_공고는_모집_상태로_거를_수_있다() throws Exception {
		posting(101, 1, "OPEN", at(12, 1, 10));
		posting(102, 2, "CLOSED", at(11, 1, 10));
		postingBookmarkRepository.save(PostingBookmarkRef.of(user.getUserId(), 101L, at(11, 1, 9)));
		postingBookmarkRepository.save(PostingBookmarkRef.of(user.getUserId(), 102L, at(11, 5, 9)));

		call("/bookmarked-postings?status=OPEN").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items.length()").value(1))
				.andExpect(jsonPath("$.data.items[0].postingId").value(101))
				.andExpect(jsonPath("$.data.totalElements").value(1));
		call("/bookmarked-postings?status=CLOSED").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items.length()").value(1))
				.andExpect(jsonPath("$.data.items[0].postingId").value(102));
	}

	@Test
	void 관심_공고는_알_수_없는_상태면_400() throws Exception {
		call("/bookmarked-postings?status=FOO")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_2"));
		call("/bookmarked-postings?status=open").andExpect(status().isBadRequest());
	}

	@Test
	void 관심_공고는_페이지로_나눠_주고_비어_있으면_빈_목록이다() throws Exception {
		call("/bookmarked-postings").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items").isEmpty())
				.andExpect(jsonPath("$.data.totalElements").value(0))
				.andExpect(jsonPath("$.data.hasNext").value(false));

		for (long id = 101; id <= 103; id++) {
			posting(id, 1, "OPEN", at(12, 1, 10));
			postingBookmarkRepository.save(PostingBookmarkRef.of(user.getUserId(), id, at(11, (int) (id - 100), 9)));
		}
		call("/bookmarked-postings?page=0&size=2").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items.length()").value(2))
				.andExpect(jsonPath("$.data.items[0].postingId").value(103))
				.andExpect(jsonPath("$.data.hasNext").value(true))
				.andExpect(jsonPath("$.data.totalPages").value(2));
	}

	@Test
	void 관심_공고는_토큰이_없으면_401() throws Exception {
		mockMvc.perform(get(BASE + "/bookmarked-postings"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("COMMON_401_1"));
	}

	// ================= 내 관심 시설 =================

	@Test
	void 관심_시설은_저장_최신순이고_모집_중_공고_수를_함께_준다() throws Exception {
		posting(101, 1, "OPEN", at(12, 1, 10));
		posting(102, 1, "OPEN", at(12, 2, 10));
		posting(103, 1, "CLOSED", at(11, 2, 10));
		posting(104, 2, "CLOSED", at(11, 2, 10));
		shelterBookmarkRepository.save(ShelterBookmarkRef.of(user.getUserId(), 1L, at(11, 1, 9)));
		shelterBookmarkRepository.save(ShelterBookmarkRef.of(user.getUserId(), 2L, at(11, 5, 9)));
		shelterBookmarkRepository.save(ShelterBookmarkRef.of(other.getUserId(), 1L, at(11, 6, 9)));

		call("/bookmarked-shelters").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items.length()").value(2))
				.andExpect(jsonPath("$.data.items[0].shelterId").value(2))
				.andExpect(jsonPath("$.data.items[0].name").value("노원 온기쉼터"))
				.andExpect(jsonPath("$.data.items[0].region.sigungu").value("노원구"))
				.andExpect(jsonPath("$.data.items[0].openPostingCount").value(0))
				.andExpect(jsonPath("$.data.items[0].bookmarkedAt").value("2026-11-05T09:00:00"))
				.andExpect(jsonPath("$.data.items[1].shelterId").value(1))
				.andExpect(jsonPath("$.data.items[1].openPostingCount").value(2))
				.andExpect(jsonPath("$.data.totalElements").value(2));
	}

	@Test
	void 관심_시설은_페이지로_나눠_주고_비어_있으면_빈_목록이다() throws Exception {
		call("/bookmarked-shelters").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items").isEmpty());

		shelterBookmarkRepository.save(ShelterBookmarkRef.of(user.getUserId(), 1L, at(11, 1, 9)));
		shelterBookmarkRepository.save(ShelterBookmarkRef.of(user.getUserId(), 2L, at(11, 2, 9)));
		call("/bookmarked-shelters?page=1&size=1").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items.length()").value(1))
				.andExpect(jsonPath("$.data.items[0].shelterId").value(1))
				.andExpect(jsonPath("$.data.hasNext").value(false))
				.andExpect(jsonPath("$.data.totalPages").value(2));
	}

	@Test
	void 관심_시설은_토큰이_없으면_401() throws Exception {
		mockMvc.perform(get(BASE + "/bookmarked-shelters"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("COMMON_401_1"));
	}

	// ================= 회원 탈퇴 연동 =================

	@Test
	void 회원_탈퇴하면_북마크와_알림은_삭제되고_신청_기록은_남는다() throws Exception {
		posting(101, 1, "OPEN", at(12, 1, 10));
		postingBookmarkRepository.save(PostingBookmarkRef.of(user.getUserId(), 101L, at(11, 1, 9)));
		postingBookmarkRepository.save(PostingBookmarkRef.of(other.getUserId(), 101L, at(11, 1, 9)));
		shelterBookmarkRepository.save(ShelterBookmarkRef.of(user.getUserId(), 1L, at(11, 1, 9)));
		shelterBookmarkRepository.save(ShelterBookmarkRef.of(other.getUserId(), 1L, at(11, 1, 9)));
		notificationRepository.save(NotificationRef.of(user.getUserId(), null, at(11, 1, 9)));
		notificationRepository.save(NotificationRef.of(other.getUserId(), null, at(11, 1, 9)));
		apply(user, 101, ApplicationStatus.COMPLETED);

		mockMvc.perform(delete(BASE).header("Authorization", "Bearer " + token)).andExpect(status().isOk());

		assertThat(postingBookmarkRepository.findAll()).extracting(PostingBookmarkRef::getUserId)
				.containsExactly(other.getUserId());
		assertThat(shelterBookmarkRepository.findAll()).extracting(ShelterBookmarkRef::getUserId)
				.containsExactly(other.getUserId());
		assertThat(notificationRepository.findAll()).extracting(NotificationRef::getUserId)
				.containsExactly(other.getUserId());
		assertThat(applicationRepository.findAll()).hasSize(1);
	}
}
