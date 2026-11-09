package com.jjonggeut.domain.volunteerexperience;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjonggeut.domain.user.entity.User;
import com.jjonggeut.domain.user.repository.UserRepository;
import com.jjonggeut.domain.volunteerexperience.repository.VolunteerExperienceRepository;
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
class VolunteerExperienceTest {

	private static final String URL = "/api/v1/users/me/volunteer-experiences";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private VolunteerExperienceRepository experienceRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private ObjectMapper objectMapper;

	private String token;
	private String otherToken;

	@BeforeEach
	void setUp() throws Exception {
		userRepository.save(User.createUser("jjong123", passwordEncoder.encode("password1"), "김쫑긋", "쫑긋이"));
		userRepository.save(User.createUser("other456", passwordEncoder.encode("password1"), "이다른", "다른이"));
		token = login("jjong123");
		otherToken = login("other456");
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

	private ResultActions create(String bearer, String body) throws Exception {
		return mockMvc.perform(post(URL)
				.header("Authorization", "Bearer " + bearer)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}

	private long createAndGetId(String bearer, String content) throws Exception {
		String response = create(bearer, """
				{"content":"%s","activityCategories":["WALK"]}
				""".formatted(content)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(response).get("data").get("experienceId").asLong();
	}

	@Test
	void 정리된_내용과_함께_저장하면_COMPLETED로_저장된다() throws Exception {
		create(token, """
				{"content":"작년 여름 보호소에서 산책과 청소를 했어요.",
				 "summary":"2025년 여름 산책·청소 봉사",
				 "activityCategories":["WALK","CLEANING","WALK"],
				 "organizationName":"행복보호소","periodText":"2025년 여름"}
				""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.data.summaryStatus").value("COMPLETED"))
				.andExpect(jsonPath("$.data.summary").value("2025년 여름 산책·청소 봉사"))
				.andExpect(jsonPath("$.data.activityCategories.length()").value(2))
				.andExpect(jsonPath("$.data.activityCategories[0]").value("WALK"))
				.andExpect(jsonPath("$.data.organizationName").value("행복보호소"));
	}

	@Test
	void 정리된_내용이_없으면_NONE으로_저장된다() throws Exception {
		create(token, """
				{"content":"강아지 산책 봉사를 했어요.","activityCategories":["WALK"]}
				""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.summaryStatus").value("NONE"))
				.andExpect(jsonPath("$.data.summary").doesNotExist());
	}

	@Test
	void 목록은_본인_항목만_최신순으로_조회된다() throws Exception {
		createAndGetId(token, "첫번째");
		createAndGetId(token, "두번째");
		createAndGetId(otherToken, "남의경험");

		String response = mockMvc.perform(get(URL).header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		JsonNode experiences = objectMapper.readTree(response).get("data").get("experiences");
		assertThat(experiences).hasSize(2);
		assertThat(experiences.get(0).get("content").asText()).isEqualTo("두번째");
		assertThat(experiences.get(1).get("content").asText()).isEqualTo("첫번째");
	}

	@Test
	void 삭제하면_목록에서_사라지고_다시_삭제하면_404이다() throws Exception {
		long id = createAndGetId(token, "삭제할경험");

		mockMvc.perform(delete(URL + "/" + id).header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());

		mockMvc.perform(get(URL).header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$.data.experiences.length()").value(0));
		mockMvc.perform(delete(URL + "/" + id).header("Authorization", "Bearer " + token))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("EXPERIENCE_404_1"));
		// soft delete 이므로 행은 남아 있다.
		assertThat(experienceRepository.findById(id)).isPresent();
	}

	@Test
	void 타인의_경험은_삭제할_수_없다() throws Exception {
		long id = createAndGetId(otherToken, "남의경험");

		mockMvc.perform(delete(URL + "/" + id).header("Authorization", "Bearer " + token))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("EXPERIENCE_404_1"));
	}

	@Test
	void 활동_분류가_비어있으면_400을_반환한다() throws Exception {
		create(token, """
				{"content":"내용","activityCategories":[]}
				""")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_2"));
	}

	@Test
	void 내용이_비어있으면_400을_반환한다() throws Exception {
		create(token, """
				{"content":"  ","activityCategories":["WALK"]}
				""")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_2"));
	}

	@Test
	void 정의되지_않은_활동_분류는_400을_반환한다() throws Exception {
		create(token, """
				{"content":"내용","activityCategories":["UNKNOWN"]}
				""")
				.andExpect(status().isBadRequest());
	}

	@Test
	void 토큰이_없으면_401을_반환한다() throws Exception {
		mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
	}
}
