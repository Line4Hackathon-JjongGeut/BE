package com.jjonggeut.domain.shelter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjonggeut.domain.recommendation.entity.ShelterRef;
import com.jjonggeut.domain.recommendation.repository.ShelterRefRepository;
import com.jjonggeut.domain.shelter.entity.ShelterAnimalStatRef;
import com.jjonggeut.domain.shelter.repository.ShelterAnimalStatRefRepository;
import com.jjonggeut.domain.user.entity.User;
import com.jjonggeut.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
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
class ShelterAnimalStatsTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ShelterRefRepository shelterRepository;

	@Autowired
	private ShelterAnimalStatRefRepository statRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private ObjectMapper objectMapper;

	private String token;

	@BeforeEach
	void setUp() throws Exception {
		userRepository.save(User.createUser("jjong123", passwordEncoder.encode("password1"), "김쫑긋", "쫑긋이"));
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
		statRepository.save(ShelterAnimalStatRef.of(1L, 24, 11, 2, LocalDateTime.of(2026, 11, 9, 18, 0)));
	}

	private ResultActions stats(Object shelterId) throws Exception {
		return mockMvc.perform(get("/api/v1/shelters/" + shelterId + "/animal-stats")
				.header("Authorization", "Bearer " + token));
	}

	@Test
	void 시설의_보호동물_현황을_보여_준다() throws Exception {
		stats(1).andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.data.shelterId").value(1))
				.andExpect(jsonPath("$.data.dogCount").value(24))
				.andExpect(jsonPath("$.data.catCount").value(11))
				.andExpect(jsonPath("$.data.otherCount").value(2))
				.andExpect(jsonPath("$.data.totalCount").value(37))
				.andExpect(jsonPath("$.data.updatedAt").value("2026-11-09T18:00:00"));
	}

	@Test
	void 등록된_현황이_없으면_모두_0이고_일시는_null이다() throws Exception {
		stats(2).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.shelterId").value(2))
				.andExpect(jsonPath("$.data.dogCount").value(0))
				.andExpect(jsonPath("$.data.catCount").value(0))
				.andExpect(jsonPath("$.data.otherCount").value(0))
				.andExpect(jsonPath("$.data.totalCount").value(0))
				.andExpect(jsonPath("$.data.updatedAt").isEmpty());
	}

	@Test
	void 없는_시설이면_404() throws Exception {
		stats(9999).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.isSuccess").value(false))
				.andExpect(jsonPath("$.code").value("SHELTER_404_1"));
	}

	@Test
	void 현황만_있고_시설이_없어도_404() throws Exception {
		statRepository.save(ShelterAnimalStatRef.of(77L, 1, 1, 1, LocalDateTime.of(2026, 11, 9, 18, 0)));

		stats(77).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("SHELTER_404_1"));
	}

	@Test
	void 시설_ID가_1_미만이면_400이고_숫자가_아니면_COMMON_400_1() throws Exception {
		stats(0).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
		stats(-1).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
		stats("abc").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_1"));
	}

	@Test
	void 토큰이_없으면_401() throws Exception {
		mockMvc.perform(get("/api/v1/shelters/1/animal-stats"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("COMMON_401_1"));
	}
}
