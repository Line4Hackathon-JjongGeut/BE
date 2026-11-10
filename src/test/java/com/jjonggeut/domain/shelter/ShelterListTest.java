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
class ShelterListTest {

	// 사용자 위치: 성북구. 노원 약 8km, 마포 약 10.5km, 고양 약 18km, 수원 약 36km, 해운대 300km 이상.
	private static final String SEONGBUK = "latitude=37.5894&longitude=127.0167";

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
		token = login();

		shelter(1, "성북 행복보호소", "서울특별시", "성북구", 37.5894, 127.0167,
				List.of(InterestAnimal.DOG, InterestAnimal.CAT), VerificationStatus.VERIFIED);
		shelter(2, "노원 온기쉼터", "서울특별시", "노원구", 37.6542, 127.0568,
				List.of(InterestAnimal.CAT), VerificationStatus.VERIFIED);
		shelter(3, "마포 다온센터", "서울특별시", "마포구", 37.5663, 126.9019,
				List.of(InterestAnimal.CAT), VerificationStatus.UNVERIFIED);
		shelter(4, "고양 희망의집", "경기도", "고양시", 37.6584, 126.8320,
				List.of(InterestAnimal.DOG), VerificationStatus.VERIFIED);
		shelter(5, "수원 보듬보호소", "경기도", "수원시", 37.2636, 127.0286,
				List.of(InterestAnimal.DOG, InterestAnimal.CAT), VerificationStatus.UNVERIFIED);
		shelter(6, "해운대 바다보호소", "부산광역시", "해운대구", 35.1631, 129.1636,
				List.of(InterestAnimal.DOG), VerificationStatus.VERIFIED);
	}

	private String login() throws Exception {
		String response = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"loginId":"jjong123","password":"password1"}
								"""))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(response).get("data").get("accessToken").asText();
	}

	private void shelter(long id, String name, String sido, String sigungu, double lat, double lon,
			List<InterestAnimal> animals, VerificationStatus verification) {
		shelterRepository.save(ShelterRef.of(id, name, sido, sigungu, sido + " " + sigungu + " 1번지", lat, lon,
				"02-000-000" + id, name + " 소개", animals, verification));
	}

	private ResultActions list(String query) throws Exception {
		return mockMvc.perform(get("/api/v1/shelters" + (query.isEmpty() ? "" : "?" + query))
				.header("Authorization", "Bearer " + token));
	}

	// ---- 기본 조회 ----

	@Test
	void 조건_없이_조회하면_이름순으로_모두_보여_준다() throws Exception {
		list("").andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.data.items.length()").value(6))
				.andExpect(jsonPath("$.data.items[0].name").value("고양 희망의집"))
				.andExpect(jsonPath("$.data.items[1].name").value("노원 온기쉼터"))
				.andExpect(jsonPath("$.data.items[5].name").value("해운대 바다보호소"))
				.andExpect(jsonPath("$.data.page").value(0))
				.andExpect(jsonPath("$.data.size").value(20))
				.andExpect(jsonPath("$.data.totalElements").value(6))
				.andExpect(jsonPath("$.data.totalPages").value(1))
				.andExpect(jsonPath("$.data.hasNext").value(false));
	}

	@Test
	void 항목에_시설_정보가_담긴다() throws Exception {
		list("sido=서울특별시&sigungu=성북구").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items.length()").value(1))
				.andExpect(jsonPath("$.data.items[0].shelterId").value(1))
				.andExpect(jsonPath("$.data.items[0].name").value("성북 행복보호소"))
				.andExpect(jsonPath("$.data.items[0].region.sido").value("서울특별시"))
				.andExpect(jsonPath("$.data.items[0].region.sigungu").value("성북구"))
				.andExpect(jsonPath("$.data.items[0].address").value("서울특별시 성북구 1번지"))
				.andExpect(jsonPath("$.data.items[0].latitude").value(37.5894))
				.andExpect(jsonPath("$.data.items[0].longitude").value(127.0167))
				.andExpect(jsonPath("$.data.items[0].phone").value("02-000-0001"))
				.andExpect(jsonPath("$.data.items[0].animalTypes[0]").value("DOG"))
				.andExpect(jsonPath("$.data.items[0].animalTypes[1]").value("CAT"))
				.andExpect(jsonPath("$.data.items[0].verificationStatus").value("VERIFIED"))
				.andExpect(jsonPath("$.data.items[0].distanceKm").isEmpty())
				.andExpect(jsonPath("$.data.items[0].openPostingCount").value(0))
				.andExpect(jsonPath("$.data.items[0].bookmarked").value(false));
	}

	@Test
	void 값이_없는_시설은_미인증이고_동물_유형이_빈_목록이다() throws Exception {
		shelterRepository.save(ShelterRef.of(7L, "정보없는 시설", null, null, null, null));

		list("verificationStatus=UNVERIFIED").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(3));
		list("sido=서울특별시").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(3));
		list("animalType=DOG").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(4));
		list("sido=서울특별시&verificationStatus=UNVERIFIED").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(1));
		list("").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items[?(@.name=='정보없는 시설')].verificationStatus").value("UNVERIFIED"))
				.andExpect(jsonPath("$.data.items[?(@.name=='정보없는 시설')].animalTypes.length()").value(0));
	}

	// ---- 지역 ----

	@Test
	void 시도로_거른다() throws Exception {
		list("sido=경기도").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(2))
				.andExpect(jsonPath("$.data.items[0].name").value("고양 희망의집"))
				.andExpect(jsonPath("$.data.items[1].name").value("수원 보듬보호소"));
	}

	@Test
	void 시군구만_보내거나_공백이면_400() throws Exception {
		list("sigungu=성북구").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_2"));
		list("sido=").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_2"));
	}

	@Test
	void 없는_지역이면_빈_목록이다() throws Exception {
		list("sido=제주특별자치도").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items").isEmpty())
				.andExpect(jsonPath("$.data.totalElements").value(0));
	}

	// ---- 인증 상태 / 동물 유형 ----

	@Test
	void 인증_상태로_거른다() throws Exception {
		list("verificationStatus=VERIFIED").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(4));
		list("verificationStatus=UNVERIFIED").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(2))
				.andExpect(jsonPath("$.data.items[0].name").value("마포 다온센터"))
				.andExpect(jsonPath("$.data.items[1].name").value("수원 보듬보호소"));
	}

	@Test
	void 보호_동물_유형으로_거른다() throws Exception {
		list("animalType=CAT").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(4));
		list("animalType=DOG").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(4));
	}

	@Test
	void 여러_조건은_모두_만족해야_한다() throws Exception {
		list("sido=서울특별시&verificationStatus=VERIFIED&animalType=CAT").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(2))
				.andExpect(jsonPath("$.data.items[0].name").value("노원 온기쉼터"))
				.andExpect(jsonPath("$.data.items[1].name").value("성북 행복보호소"));
	}

	@Test
	void 알_수_없는_인증_상태나_동물_유형은_400() throws Exception {
		list("verificationStatus=GOOD").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_2"));
		list("verificationStatus=verified").andExpect(status().isBadRequest());
		list("animalType=BIRD").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_2"));
	}

	// ---- 거리 ----

	@Test
	void 좌표를_보내면_가까운_순으로_거리와_함께_보여_준다() throws Exception {
		list(SEONGBUK).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(6))
				.andExpect(jsonPath("$.data.items[0].name").value("성북 행복보호소"))
				.andExpect(jsonPath("$.data.items[0].distanceKm").value(0.0))
				.andExpect(jsonPath("$.data.items[1].name").value("노원 온기쉼터"))
				.andExpect(jsonPath("$.data.items[2].name").value("마포 다온센터"))
				.andExpect(jsonPath("$.data.items[3].name").value("고양 희망의집"))
				.andExpect(jsonPath("$.data.items[4].name").value("수원 보듬보호소"))
				.andExpect(jsonPath("$.data.items[5].name").value("해운대 바다보호소"))
				.andExpect(jsonPath("$.data.items[1].distanceKm").value(org.hamcrest.Matchers.closeTo(8.0, 1.0)))
				.andExpect(jsonPath("$.data.items[5].distanceKm").value(org.hamcrest.Matchers.greaterThan(300.0)));
	}

	@Test
	void 최대_거리_안의_시설만_보여_준다() throws Exception {
		list(SEONGBUK + "&maxDistanceKm=10").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(2))
				.andExpect(jsonPath("$.data.items[0].name").value("성북 행복보호소"))
				.andExpect(jsonPath("$.data.items[1].name").value("노원 온기쉼터"));
		list(SEONGBUK + "&maxDistanceKm=25").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(4));
		list(SEONGBUK + "&maxDistanceKm=1").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(1));
	}

	@Test
	void 거리와_다른_조건을_함께_쓴다() throws Exception {
		list(SEONGBUK + "&maxDistanceKm=25&animalType=DOG").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(2))
				.andExpect(jsonPath("$.data.items[0].name").value("성북 행복보호소"))
				.andExpect(jsonPath("$.data.items[1].name").value("고양 희망의집"));
	}

	@Test
	void 좌표가_없는_시설은_거리_조건에서_빠지고_좌표만_보내면_맨_뒤에_온다() throws Exception {
		shelterRepository.save(ShelterRef.of(7L, "좌표없는 시설", "서울특별시", "중구", null, null));

		list(SEONGBUK + "&maxDistanceKm=200").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(5));
		list(SEONGBUK).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(7))
				.andExpect(jsonPath("$.data.items[6].name").value("좌표없는 시설"))
				.andExpect(jsonPath("$.data.items[6].distanceKm").isEmpty());
	}

	@Test
	void 좌표는_함께_보내야_하고_거리는_좌표가_있어야_한다() throws Exception {
		list("latitude=37.5").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_2"));
		list("longitude=127.0").andExpect(status().isBadRequest());
		list("maxDistanceKm=10").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_2"));
	}

	@Test
	void 좌표와_거리_범위가_틀리면_400() throws Exception {
		list("latitude=91&longitude=127").andExpect(status().isBadRequest());
		list("latitude=37&longitude=181").andExpect(status().isBadRequest());
		list(SEONGBUK + "&maxDistanceKm=0").andExpect(status().isBadRequest());
		list(SEONGBUK + "&maxDistanceKm=201").andExpect(status().isBadRequest());
		list("latitude=abc&longitude=127").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_1"));
	}

	// ---- 페이지 ----

	@Test
	void 페이지로_나눠_준다() throws Exception {
		list("page=0&size=4").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items.length()").value(4))
				.andExpect(jsonPath("$.data.totalElements").value(6))
				.andExpect(jsonPath("$.data.totalPages").value(2))
				.andExpect(jsonPath("$.data.hasNext").value(true));
		list("page=1&size=4").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items.length()").value(2))
				.andExpect(jsonPath("$.data.hasNext").value(false));
		list("page=5&size=4").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items").isEmpty())
				.andExpect(jsonPath("$.data.totalElements").value(6));
	}

	@Test
	void 거리순_목록도_페이지로_나눈다() throws Exception {
		list(SEONGBUK + "&page=1&size=2").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items.length()").value(2))
				.andExpect(jsonPath("$.data.items[0].name").value("마포 다온센터"))
				.andExpect(jsonPath("$.data.items[1].name").value("고양 희망의집"))
				.andExpect(jsonPath("$.data.totalPages").value(3));
	}

	@Test
	void page와_size_범위가_틀리면_400이고_숫자가_아니면_COMMON_400_1() throws Exception {
		list("page=-1").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
		list("size=0").andExpect(status().isBadRequest());
		list("size=51").andExpect(status().isBadRequest());
		list("page=abc").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_1"));
	}

	// ---- 모집 중 공고 수 / 관심 저장 여부 ----

	@Test
	void 모집_중_공고_수와_내_관심_저장_여부를_함께_준다() throws Exception {
		postingRepository.save(PostingRef.of(101L, 1L, "VOLUNTEER", "공고1", "OPEN",
				List.of(ActivityCategory.WALK), "DOG", LocalDateTime.of(2026, 12, 1, 10, 0), 10, 1));
		postingRepository.save(PostingRef.of(102L, 1L, "SPONSOR", "공고2", "OPEN",
				List.of(ActivityCategory.ETC), "ALL", null, null, null));
		postingRepository.save(PostingRef.of(103L, 1L, "VOLUNTEER", "공고3", "CLOSED",
				List.of(ActivityCategory.WALK), "DOG", LocalDateTime.of(2026, 12, 1, 10, 0), 10, 10));
		bookmarkRepository.save(ShelterBookmarkRef.of(user.getUserId(), 1L, LocalDateTime.of(2026, 11, 1, 9, 0)));
		bookmarkRepository.save(ShelterBookmarkRef.of(other.getUserId(), 2L, LocalDateTime.of(2026, 11, 1, 9, 0)));

		list("sido=서울특별시").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items[?(@.shelterId==1)].openPostingCount").value(2))
				.andExpect(jsonPath("$.data.items[?(@.shelterId==1)].bookmarked").value(true))
				.andExpect(jsonPath("$.data.items[?(@.shelterId==2)].openPostingCount").value(0))
				.andExpect(jsonPath("$.data.items[?(@.shelterId==2)].bookmarked").value(false))
				.andExpect(jsonPath("$.data.items[?(@.shelterId==3)].bookmarked").value(false));
	}

	// ---- 인증 ----

	@Test
	void 토큰이_없으면_401() throws Exception {
		mockMvc.perform(get("/api/v1/shelters"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("COMMON_401_1"));
	}
}
