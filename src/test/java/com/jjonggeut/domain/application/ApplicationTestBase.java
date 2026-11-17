package com.jjonggeut.domain.application;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjonggeut.domain.activity.entity.ApplicationStatus;
import com.jjonggeut.domain.activity.entity.VolunteerApplicationRef;
import com.jjonggeut.domain.activity.repository.VolunteerApplicationRefRepository;
import com.jjonggeut.domain.application.entity.ApplicationStatusHistory;
import com.jjonggeut.domain.application.entity.StatusActor;
import com.jjonggeut.domain.application.repository.ApplicationStatusHistoryRepository;
import com.jjonggeut.domain.posting.entity.PostingSchedule;
import com.jjonggeut.domain.posting.repository.PostingScheduleRepository;
import com.jjonggeut.domain.profile.entity.InterestAnimal;
import com.jjonggeut.domain.recommendation.entity.PostingRef;
import com.jjonggeut.domain.recommendation.entity.ShelterRef;
import com.jjonggeut.domain.recommendation.repository.PostingRefRepository;
import com.jjonggeut.domain.recommendation.repository.ShelterRefRepository;
import com.jjonggeut.domain.shelter.entity.VerificationStatus;
import com.jjonggeut.domain.user.entity.User;
import com.jjonggeut.domain.user.repository.UserRepository;
import com.jjonggeut.domain.volunteerexperience.entity.ActivityCategory;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * 봉사신청 API 테스트가 함께 쓰는 도우미. 시설 1(인증)·2(미인증), 공고 501(모집 중)·502(마감)·503(비공개)·504(일정 없는 예전 공고).
 * 정원 숫자는 DB 에서 직접 올리고 내리므로, 확인하기 전에 {@link #refresh()} 로 영속성 컨텍스트를 비운다.
 */
abstract class ApplicationTestBase {

	static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

	@Autowired
	MockMvc mockMvc;

	@Autowired
	UserRepository userRepository;

	@Autowired
	ShelterRefRepository shelterRepository;

	@Autowired
	PostingRefRepository postingRepository;

	@Autowired
	PostingScheduleRepository scheduleRepository;

	@Autowired
	VolunteerApplicationRefRepository applicationRepository;

	@Autowired
	ApplicationStatusHistoryRepository historyRepository;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	EntityManager entityManager;

	User user;
	User other;
	String token;
	String otherToken;

	@BeforeEach
	void baseSetUp() throws Exception {
		user = userRepository.save(User.createUser("jjong123", passwordEncoder.encode("password1"), "김쫑긋", "쫑긋이"));
		other = userRepository.save(User.createUser("other456", passwordEncoder.encode("password1"), "이다른", "다른이"));
		token = login("jjong123");
		otherToken = login("other456");

		shelter(1, "성북 행복보호소", VerificationStatus.VERIFIED);
		shelter(2, "노원 온기쉼터", VerificationStatus.UNVERIFIED);
		List<PostingRef> postings = new ArrayList<>();
		postings.add(PostingRef.of(501L, 1L, "VOLUNTEER", "주말 산책 봉사", "OPEN", List.of(ActivityCategory.WALK), "DOG",
				null, 10, 0).withPreparations(List.of("편한 복장", "운동화")));
		postings.add(PostingRef.of(502L, 1L, "VOLUNTEER", "마감된 봉사", "CLOSED", List.of(ActivityCategory.WALK), "DOG",
				null, 10, 0));
		postings.add(PostingRef.of(503L, 1L, "VOLUNTEER", "비공개 봉사", "OPEN", List.of(ActivityCategory.WALK), "DOG",
				null, 10, 0).withPublic(false));
		postings.add(PostingRef.of(504L, 2L, "VOLUNTEER", "예전 봉사", "OPEN", List.of(ActivityCategory.FEEDING_CARE), "CAT",
				LocalDateTime.now().plusDays(10).withNano(0), 10, 2)
				.withPeriod(null, LocalDateTime.now().plusDays(10).plusHours(2).withNano(0)));
		postingRepository.saveAll(postings);
		postingRepository.flush();
	}

	String login(String loginId) throws Exception {
		String response = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"loginId\":\"" + loginId + "\",\"password\":\"password1\"}"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(response).get("data").get("accessToken").asText();
	}

	/** 내 토큰으로 보낸다. 본문이 null 이면 본문 없이 보낸다. */
	ResultActions call(MockHttpServletRequestBuilder builder, String body) throws Exception {
		return call(builder, token, body);
	}

	ResultActions call(MockHttpServletRequestBuilder builder, String bearer, String body) throws Exception {
		builder.header("Authorization", "Bearer " + bearer);
		if (body != null) {
			builder.contentType(MediaType.APPLICATION_JSON).content(body);
		}
		return mockMvc.perform(builder);
	}

	void shelter(long id, String name, VerificationStatus verification) {
		shelterRepository.save(ShelterRef.of(id, name, "서울특별시", "성북구", "서울특별시 성북구 " + id + "번지", 37.58, 127.01,
				"02-000-000" + id, name + " 소개", List.of(InterestAnimal.DOG, InterestAnimal.CAT), verification));
	}

	/** 일정을 만들고 ID 를 돌려준다. startAfterHours 는 지금부터 시작까지의 시간이다. */
	Long schedule(long postingId, long startAfterHours, int capacity, int applied) {
		LocalDateTime start = LocalDateTime.now().plusHours(startAfterHours).withNano(0);
		return scheduleRepository.saveAndFlush(PostingSchedule.of(postingId, start, start.plusHours(2), capacity, applied))
				.getScheduleId();
	}

	/** 신청을 만들어 두고, 최초 접수 이력도 남긴다. */
	Long application(User owner, long postingId, Long scheduleId, ApplicationStatus status) {
		VolunteerApplicationRef saved = applicationRepository.saveAndFlush(VolunteerApplicationRef.of(owner.getUserId(),
				postingId, scheduleId, status, LocalDateTime.now().minusDays(1).withNano(0)));
		historyRepository.saveAndFlush(ApplicationStatusHistory.of(saved.getApplicationId(), null,
				ApplicationStatus.PENDING, StatusActor.USER, null, saved.getAppliedAt()));
		return saved.getApplicationId();
	}

	/** DB 에서 직접 바꾼 정원 숫자를 다시 읽도록 영속성 컨텍스트를 비운다. */
	void refresh() {
		entityManager.flush();
		entityManager.clear();
	}

	int appliedCount(Long scheduleId) {
		refresh();
		return scheduleRepository.findById(scheduleId).orElseThrow().getAppliedCount();
	}

	int applicantCount(long postingId) {
		refresh();
		return postingRepository.findById(postingId).orElseThrow().getApplicantCount();
	}
}
