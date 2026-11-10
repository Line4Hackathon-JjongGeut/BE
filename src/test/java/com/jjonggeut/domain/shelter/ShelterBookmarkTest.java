package com.jjonggeut.domain.shelter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjonggeut.domain.activity.entity.ShelterBookmarkRef;
import com.jjonggeut.domain.activity.repository.ShelterBookmarkRefRepository;
import com.jjonggeut.domain.recommendation.entity.ShelterRef;
import com.jjonggeut.domain.recommendation.repository.ShelterRefRepository;
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
class ShelterBookmarkTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ShelterRefRepository shelterRepository;

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

		shelterRepository.save(ShelterRef.of(1L, "성북 행복보호소", "서울특별시", "성북구", 37.5894, 127.0167));
		shelterRepository.save(ShelterRef.of(2L, "노원 온기쉼터", "서울특별시", "노원구", 37.6542, 127.0568));
	}

	private ResultActions save(Object shelterId) throws Exception {
		return mockMvc.perform(post("/api/v1/shelters/" + shelterId + "/bookmarks")
				.header("Authorization", "Bearer " + token));
	}

	private ResultActions remove(Object shelterId) throws Exception {
		return mockMvc.perform(delete("/api/v1/shelters/" + shelterId + "/bookmarks")
				.header("Authorization", "Bearer " + token));
	}

	private ResultActions detail(long shelterId) throws Exception {
		return mockMvc.perform(get("/api/v1/shelters/" + shelterId).header("Authorization", "Bearer " + token));
	}

	@Test
	void 관심_시설로_저장한다() throws Exception {
		save(1).andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.data.shelterId").value(1))
				.andExpect(jsonPath("$.data.bookmarked").value(true))
				.andExpect(jsonPath("$.data.createdAt").isNotEmpty());

		org.junit.jupiter.api.Assertions.assertTrue(
				bookmarkRepository.existsByUserIdAndShelterId(user.getUserId(), 1L));
		detail(1).andExpect(jsonPath("$.data.bookmarked").value(true));
		detail(2).andExpect(jsonPath("$.data.bookmarked").value(false));
	}

	@Test
	void 이미_저장한_시설이면_409이고_한_건만_남는다() throws Exception {
		save(1).andExpect(status().isOk());

		save(1).andExpect(status().isConflict())
				.andExpect(jsonPath("$.isSuccess").value(false))
				.andExpect(jsonPath("$.code").value("SHELTER_409_1"));
		org.junit.jupiter.api.Assertions.assertEquals(1, bookmarkRepository.findAll().stream()
				.filter(b -> b.getUserId().equals(user.getUserId())).count());
	}

	@Test
	void 다른_사용자가_저장한_시설도_내가_저장할_수_있다() throws Exception {
		bookmarkRepository.save(ShelterBookmarkRef.of(other.getUserId(), 1L, LocalDateTime.of(2026, 11, 1, 9, 0)));

		save(1).andExpect(status().isOk());
		org.junit.jupiter.api.Assertions.assertEquals(2, bookmarkRepository.findAll().size());
	}

	@Test
	void 저장한_시설을_해제한다() throws Exception {
		save(1).andExpect(status().isOk());

		remove(1).andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.code").value("COMMON_200"));
		org.junit.jupiter.api.Assertions.assertFalse(
				bookmarkRepository.existsByUserIdAndShelterId(user.getUserId(), 1L));
		detail(1).andExpect(jsonPath("$.data.bookmarked").value(false));
	}

	@Test
	void 해제한_뒤에는_다시_저장할_수_있다() throws Exception {
		save(1).andExpect(status().isOk());
		remove(1).andExpect(status().isOk());

		save(1).andExpect(status().isOk());
	}

	@Test
	void 저장하지_않은_시설을_해제하면_404() throws Exception {
		remove(1).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("SHELTER_404_2"));
		save(1).andExpect(status().isOk());
		remove(1).andExpect(status().isOk());
		remove(1).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("SHELTER_404_2"));
	}

	@Test
	void 해제는_내_저장만_지운다() throws Exception {
		bookmarkRepository.save(ShelterBookmarkRef.of(other.getUserId(), 1L, LocalDateTime.of(2026, 11, 1, 9, 0)));

		remove(1).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("SHELTER_404_2"));
		org.junit.jupiter.api.Assertions.assertTrue(
				bookmarkRepository.existsByUserIdAndShelterId(other.getUserId(), 1L));
	}

	@Test
	void 없는_시설이면_저장도_해제도_404() throws Exception {
		save(9999).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("SHELTER_404_1"));
		remove(9999).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("SHELTER_404_1"));
	}

	@Test
	void 시설_ID가_1_미만이면_400이고_숫자가_아니면_COMMON_400_1() throws Exception {
		save(0).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
		remove(-1).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
		save("abc").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_1"));
		remove("abc").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_1"));
	}

	@Test
	void 토큰이_없으면_401() throws Exception {
		mockMvc.perform(post("/api/v1/shelters/1/bookmarks"))
				.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("COMMON_401_1"));
		mockMvc.perform(delete("/api/v1/shelters/1/bookmarks"))
				.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("COMMON_401_1"));
	}
}
