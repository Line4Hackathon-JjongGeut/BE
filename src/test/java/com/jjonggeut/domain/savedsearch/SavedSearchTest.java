package com.jjonggeut.domain.savedsearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjonggeut.domain.profile.entity.AvailableDay;
import com.jjonggeut.domain.savedsearch.entity.SavedSearch;
import com.jjonggeut.domain.savedsearch.repository.SavedSearchRepository;
import com.jjonggeut.domain.user.entity.User;
import com.jjonggeut.domain.user.repository.UserRepository;
import com.jjonggeut.domain.volunteerexperience.entity.ActivityCategory;
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
class SavedSearchTest {

	private static final String BASE = "/api/v1/users/me/saved-searches";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private SavedSearchRepository savedSearchRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private ObjectMapper objectMapper;

	private User user;
	private User other;
	private String token;
	private String otherToken;

	@BeforeEach
	void setUp() throws Exception {
		user = userRepository.save(User.createUser("jjong123", passwordEncoder.encode("password1"), "김쫑긋", "쫑긋이"));
		other = userRepository.save(User.createUser("other456", passwordEncoder.encode("password1"), "이다른", "다른이"));
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
		return mockMvc.perform(post(BASE)
				.header("Authorization", "Bearer " + bearer)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}

	private ResultActions update(String bearer, Object id, String body) throws Exception {
		return mockMvc.perform(patch(BASE + "/" + id)
				.header("Authorization", "Bearer " + bearer)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}

	private ResultActions remove(String bearer, Object id) throws Exception {
		return mockMvc.perform(delete(BASE + "/" + id).header("Authorization", "Bearer " + bearer));
	}

	private ResultActions list(String bearer) throws Exception {
		return mockMvc.perform(get(BASE).header("Authorization", "Bearer " + bearer));
	}

	private SavedSearch saved(User owner, String name, boolean isDefault) {
		return savedSearchRepository.save(SavedSearch.create(owner.getUserId(), name, "서울특별시", "성북구", null, null,
				10, List.of(AvailableDay.SAT), List.of(), List.of(ActivityCategory.WALK), isDefault));
	}

	private SavedSearch reload(SavedSearch search) {
		savedSearchRepository.flush();
		return savedSearchRepository.findById(search.getSavedSearchId()).orElseThrow();
	}

	// ---- 검색 조건 저장 ----

	@Test
	void 검색_조건을_저장한다() throws Exception {
		create(token, """
				{"name":"주말 산책","region":{"sido":"서울특별시","sigungu":"성북구","latitude":37.5894,"longitude":127.0167},
				 "maxDistanceKm":10,"availableDays":["SAT","SUN"],"availableTimeSlots":["MORNING"],
				 "activityCategories":["WALK","FEEDING_CARE"]}
				""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.data.savedSearchId").isNotEmpty())
				.andExpect(jsonPath("$.data.name").value("주말 산책"))
				.andExpect(jsonPath("$.data.region.sido").value("서울특별시"))
				.andExpect(jsonPath("$.data.region.sigungu").value("성북구"))
				.andExpect(jsonPath("$.data.region.latitude").value(37.5894))
				.andExpect(jsonPath("$.data.maxDistanceKm").value(10))
				.andExpect(jsonPath("$.data.availableDays[0]").value("SAT"))
				.andExpect(jsonPath("$.data.availableDays[1]").value("SUN"))
				.andExpect(jsonPath("$.data.availableTimeSlots[0]").value("MORNING"))
				.andExpect(jsonPath("$.data.activityCategories[1]").value("FEEDING_CARE"))
				.andExpect(jsonPath("$.data.isDefault").value(false))
				.andExpect(jsonPath("$.data.createdAt").isNotEmpty());

		List<SavedSearch> saved = savedSearchRepository.findAll();
		assertThat(saved).hasSize(1);
		assertThat(saved.get(0).getUserId()).isEqualTo(user.getUserId());
	}

	@Test
	void 조건_하나만으로도_저장할_수_있고_나머지는_비어_있다() throws Exception {
		create(token, """
				{"name":"가까운 곳","maxDistanceKm":5}
				""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.region").isEmpty())
				.andExpect(jsonPath("$.data.maxDistanceKm").value(5))
				.andExpect(jsonPath("$.data.availableDays").isEmpty())
				.andExpect(jsonPath("$.data.activityCategories").isEmpty());
	}

	@Test
	void 배열은_중복이_제거되고_이름은_앞뒤_공백이_잘린다() throws Exception {
		create(token, """
				{"name":"  산책  ","availableDays":["SAT","SAT","SUN"]}
				""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.name").value("산책"))
				.andExpect(jsonPath("$.data.availableDays.length()").value(2));
	}

	@Test
	void 기본으로_저장하면_기존_기본은_해제된다() throws Exception {
		SavedSearch first = saved(user, "기존 기본", true);

		create(token, """
				{"name":"새 기본","maxDistanceKm":3,"isDefault":true}
				""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.isDefault").value(true));

		assertThat(reload(first).isDefaultSearch()).isFalse();
		assertThat(savedSearchRepository.findByUserIdAndDefaultSearchTrue(user.getUserId())).hasSize(1);
	}

	@Test
	void 같은_이름이_있으면_409이고_다른_사용자와는_겹쳐도_된다() throws Exception {
		saved(user, "산책", false);

		create(token, """
				{"name":"산책","maxDistanceKm":3}
				""")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("SAVED_SEARCH_409_1"));

		create(otherToken, """
				{"name":"산책","maxDistanceKm":3}
				""").andExpect(status().isOk());
	}

	@Test
	void 열_개를_넘으면_409() throws Exception {
		for (int i = 1; i <= 10; i++) {
			saved(user, "조건" + i, false);
		}

		create(token, """
				{"name":"열한번째","maxDistanceKm":3}
				""")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("SAVED_SEARCH_409_2"));

		assertThat(savedSearchRepository.countByUserId(user.getUserId())).isEqualTo(10);
	}

	@Test
	void 저장은_이름이_없거나_길거나_공백이면_400() throws Exception {
		create(token, """
				{"maxDistanceKm":3}
				""").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
		create(token, """
				{"name":"   ","maxDistanceKm":3}
				""").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
		create(token, """
				{"name":"이름이스무자를넘어가는아주긴검색조건이름입니다","maxDistanceKm":3}
				""").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
	}

	@Test
	void 지정한_조건이_하나도_없으면_400() throws Exception {
		create(token, """
				{"name":"빈 조건"}
				""").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
		create(token, """
				{"name":"빈 배열","availableDays":[],"activityCategories":[]}
				""").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));

		assertThat(savedSearchRepository.findAll()).isEmpty();
	}

	@Test
	void 저장은_값_형식이_틀리면_400() throws Exception {
		create(token, """
				{"name":"거리","maxDistanceKm":0}
				""").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
		create(token, """
				{"name":"거리","maxDistanceKm":201}
				""").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
		create(token, """
				{"name":"지역","region":{"sido":"서울특별시"}}
				""").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
		create(token, """
				{"name":"좌표","region":{"sido":"서울특별시","sigungu":"성북구","latitude":37.5}}
				""").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
		create(token, """
				{"name":"요일","availableDays":["FUNDAY"]}
				""").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_1"));
		create(token, "{").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_1"));
	}

	@Test
	void 저장은_토큰이_없으면_401() throws Exception {
		mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("""
						{"name":"산책","maxDistanceKm":3}
						"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("COMMON_401_1"));
	}

	// ---- 목록 조회 ----

	@Test
	void 저장한_게_없으면_빈_목록이다() throws Exception {
		list(token).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.savedSearches").isEmpty());
	}

	@Test
	void 기본_조건이_맨_앞이고_나머지는_최근_순이다() throws Exception {
		SavedSearch older = saved(user, "오래된", false);
		SavedSearch newer = saved(user, "최근", false);
		SavedSearch defaultOne = saved(user, "기본", true);
		saved(other, "남의 것", true);

		list(token).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.savedSearches.length()").value(3))
				.andExpect(jsonPath("$.data.savedSearches[0].savedSearchId").value(defaultOne.getSavedSearchId()))
				.andExpect(jsonPath("$.data.savedSearches[0].isDefault").value(true))
				.andExpect(jsonPath("$.data.savedSearches[1].savedSearchId").value(newer.getSavedSearchId()))
				.andExpect(jsonPath("$.data.savedSearches[2].savedSearchId").value(older.getSavedSearchId()));
	}

	@Test
	void 목록은_토큰이_없으면_401() throws Exception {
		mockMvc.perform(get(BASE))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("COMMON_401_1"));
	}

	// ---- 수정 ----

	@Test
	void 이름과_필터를_수정한다() throws Exception {
		SavedSearch search = saved(user, "산책", false);

		update(token, search.getSavedSearchId(), """
				{"name":"평일 저녁","maxDistanceKm":20,"availableDays":["MON","TUE"],"availableTimeSlots":["EVENING"],
				 "region":{"sido":"경기도","sigungu":"고양시"}}
				""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.name").value("평일 저녁"))
				.andExpect(jsonPath("$.data.maxDistanceKm").value(20))
				.andExpect(jsonPath("$.data.availableDays[0]").value("MON"))
				.andExpect(jsonPath("$.data.availableTimeSlots[0]").value("EVENING"))
				.andExpect(jsonPath("$.data.region.sido").value("경기도"))
				.andExpect(jsonPath("$.data.activityCategories[0]").value("WALK"));

		SavedSearch changed = reload(search);
		assertThat(changed.getName()).isEqualTo("평일 저녁");
		assertThat(changed.getRegionSigungu()).isEqualTo("고양시");
	}

	@Test
	void 보낸_항목만_바뀌고_배열은_빈_배열로_비울_수_있다() throws Exception {
		SavedSearch search = saved(user, "산책", false);

		update(token, search.getSavedSearchId(), """
				{"availableDays":[]}
				""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.availableDays").isEmpty())
				.andExpect(jsonPath("$.data.name").value("산책"))
				.andExpect(jsonPath("$.data.maxDistanceKm").value(10))
				.andExpect(jsonPath("$.data.activityCategories[0]").value("WALK"));
	}

	@Test
	void 기본_여부를_바꾸면_기존_기본이_해제된다() throws Exception {
		SavedSearch a = saved(user, "A", true);
		SavedSearch b = saved(user, "B", false);

		update(token, b.getSavedSearchId(), """
				{"isDefault":true}
				""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.isDefault").value(true));

		assertThat(reload(a).isDefaultSearch()).isFalse();
		assertThat(reload(b).isDefaultSearch()).isTrue();
	}

	@Test
	void 기본_조건을_해제할_수_있다() throws Exception {
		SavedSearch a = saved(user, "A", true);

		update(token, a.getSavedSearchId(), """
				{"isDefault":false}
				""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.isDefault").value(false));
	}

	@Test
	void 내_이름을_그대로_보내도_성공하고_다른_조건과_겹치면_409() throws Exception {
		SavedSearch a = saved(user, "A", false);
		saved(user, "B", false);

		update(token, a.getSavedSearchId(), """
				{"name":"A"}
				""").andExpect(status().isOk());
		update(token, a.getSavedSearchId(), """
				{"name":"B"}
				""")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("SAVED_SEARCH_409_1"));

		assertThat(reload(a).getName()).isEqualTo("A");
	}

	@Test
	void 수정으로_조건이_하나도_남지_않으면_400이고_바뀌지_않는다() throws Exception {
		SavedSearch search = savedSearchRepository.save(SavedSearch.create(user.getUserId(), "하나뿐", null, null,
				null, null, null, List.of(AvailableDay.SAT), List.of(), List.of(), false));

		update(token, search.getSavedSearchId(), """
				{"availableDays":[],"name":"새 이름"}
				""")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_2"));

		SavedSearch unchanged = reload(search);
		assertThat(unchanged.getName()).isEqualTo("하나뿐");
		assertThat(unchanged.getAvailableDays()).containsExactly(AvailableDay.SAT);
	}

	@Test
	void 수정할_항목이_없거나_형식이_틀리면_400() throws Exception {
		SavedSearch search = saved(user, "산책", false);

		update(token, search.getSavedSearchId(), "{}")
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
		update(token, search.getSavedSearchId(), """
				{"maxDistanceKm":500}
				""").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
		update(token, search.getSavedSearchId(), """
				{"name":" 앞공백"}
				""").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
		update(token, search.getSavedSearchId(), "{")
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_1"));
	}

	@Test
	void 없거나_남의_조건_수정은_404() throws Exception {
		SavedSearch mine = saved(user, "내 것", false);
		SavedSearch others = saved(other, "남의 것", false);

		update(token, 999999L, """
				{"name":"변경"}
				""").andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("SAVED_SEARCH_404_1"));
		update(token, others.getSavedSearchId(), """
				{"name":"변경"}
				""").andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("SAVED_SEARCH_404_1"));
		update(otherToken, mine.getSavedSearchId(), """
				{"name":"변경"}
				""").andExpect(status().isNotFound());

		assertThat(reload(others).getName()).isEqualTo("남의 것");
		assertThat(reload(mine).getName()).isEqualTo("내 것");
	}

	@Test
	void 수정은_ID가_숫자가_아니면_400_토큰이_없으면_401() throws Exception {
		update(token, "abc", """
				{"name":"변경"}
				""").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_1"));
		mockMvc.perform(patch(BASE + "/1").contentType(MediaType.APPLICATION_JSON).content("""
						{"name":"변경"}
						"""))
				.andExpect(status().isUnauthorized());
	}

	// ---- 삭제 ----

	@Test
	void 저장_검색_조건을_삭제한다() throws Exception {
		SavedSearch search = saved(user, "산책", false);
		SavedSearch keep = saved(user, "유지", false);

		remove(token, search.getSavedSearchId())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true));

		assertThat(savedSearchRepository.findById(search.getSavedSearchId())).isEmpty();
		assertThat(savedSearchRepository.findById(keep.getSavedSearchId())).isPresent();
	}

	@Test
	void 기본_조건을_지워도_다른_조건이_기본이_되지는_않는다() throws Exception {
		SavedSearch defaultOne = saved(user, "기본", true);
		SavedSearch normal = saved(user, "일반", false);

		remove(token, defaultOne.getSavedSearchId()).andExpect(status().isOk());

		assertThat(reload(normal).isDefaultSearch()).isFalse();
	}

	@Test
	void 없거나_남의_조건_삭제는_404이고_지워지지_않는다() throws Exception {
		SavedSearch others = saved(other, "남의 것", false);

		remove(token, 999999L)
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("SAVED_SEARCH_404_1"));
		remove(token, others.getSavedSearchId())
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("SAVED_SEARCH_404_1"));

		assertThat(savedSearchRepository.findById(others.getSavedSearchId())).isPresent();
	}

	@Test
	void 삭제는_ID가_숫자가_아니면_400_토큰이_없으면_401() throws Exception {
		remove(token, "abc").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_1"));
		mockMvc.perform(delete(BASE + "/1"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("COMMON_401_1"));
	}

	// ---- 회원 탈퇴 연동 ----

	@Test
	void 회원_탈퇴하면_저장한_검색_조건도_삭제된다() throws Exception {
		saved(user, "산책", true);
		SavedSearch others = saved(other, "남의 것", false);

		mockMvc.perform(delete("/api/v1/users/me").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());

		assertThat(savedSearchRepository.countByUserId(user.getUserId())).isZero();
		assertThat(savedSearchRepository.findById(others.getSavedSearchId())).isPresent();
	}
}
