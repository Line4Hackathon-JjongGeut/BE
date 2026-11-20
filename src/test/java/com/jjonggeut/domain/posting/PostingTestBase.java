package com.jjonggeut.domain.posting;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** 모집공고 API 테스트가 함께 쓰는 로그인·테스트 데이터 도우미. 각 테스트 클래스에서 스프링 테스트 어노테이션을 붙인다. */
abstract class PostingTestBase {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	UserRepository userRepository;

	@Autowired
	ShelterRefRepository shelterRepository;

	@Autowired
	PostingRefRepository postingRepository;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Autowired
	ObjectMapper objectMapper;

	User user;
	User other;
	String token;
	String otherToken;

	private final List<PostingRef> pending = new ArrayList<>();

	@BeforeEach
	void baseSetUp() throws Exception {
		user = userRepository.save(User.createUser("jjong123", passwordEncoder.encode("password1"), "김쫑긋", "쫑긋이"));
		other = userRepository.save(User.createUser("other456", passwordEncoder.encode("password1"), "이다른", "다른이"));
		token = login("jjong123");
		otherToken = login("other456");
	}

	String login(String loginId) throws Exception {
		String response = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"loginId\":\"" + loginId + "\",\"password\":\"password1\"}"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(response).get("data").get("accessToken").asText();
	}

	ResultActions get(String url) throws Exception {
		return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url).header("Authorization", "Bearer " + token));
	}

	/** 본문이 null 이면 본문 없이 보낸다. */
	ResultActions send(MockHttpServletRequestBuilder builder, String body) throws Exception {
		builder.header("Authorization", "Bearer " + token);
		if (body != null) {
			builder.contentType(MediaType.APPLICATION_JSON).content(body);
		}
		return mockMvc.perform(builder);
	}

	void shelter(long id, String name, String sido, String sigungu, double lat, double lon,
			VerificationStatus verification) {
		shelterRepository.save(ShelterRef.of(id, name, sido, sigungu, sido + " " + sigungu + " 1번지", lat, lon,
				"02-000-000" + id, name + " 소개", List.of(InterestAnimal.DOG, InterestAnimal.CAT), verification));
	}

	/** 공고를 만들어 두고, 상세 값을 채운 뒤 {@link #savePostings()} 로 한꺼번에 저장한다. */
	PostingRef posting(long id, long shelterId, String type, String status, String title,
			List<ActivityCategory> categories, String animal, LocalDateTime startAt, Integer capacity,
			Integer applicants) {
		PostingRef posting = PostingRef.of(id, shelterId, type, title, status, categories, animal, startAt, capacity,
				applicants);
		pending.add(posting);
		return posting;
	}

	void savePostings() {
		postingRepository.saveAll(pending);
		postingRepository.flush();
		pending.clear();
	}
}
