package com.jjonggeut.domain.posting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jjonggeut.domain.activity.entity.PostingBookmarkRef;
import com.jjonggeut.domain.activity.repository.PostingBookmarkRefRepository;
import com.jjonggeut.domain.shelter.entity.VerificationStatus;
import com.jjonggeut.domain.volunteerexperience.entity.ActivityCategory;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PostingSearchTest extends PostingTestBase {

	@Autowired
	PostingBookmarkRefRepository bookmarkRepository;

	static final String SEONGBUK = "latitude=37.5894&longitude=127.0167";

	@BeforeEach
	void setUpData() {
		shelter(1, "성북 행복보호소", "서울특별시", "성북구", 37.5894, 127.0167, VerificationStatus.VERIFIED);
		shelter(2, "노원 온기쉼터", "서울특별시", "노원구", 37.6542, 127.0568, VerificationStatus.VERIFIED);
		shelter(3, "고양 희망의집", "경기도", "고양시", 37.6584, 126.8320, VerificationStatus.UNVERIFIED);
		shelter(4, "해운대 바다보호소", "부산광역시", "해운대구", 35.1631, 129.1636, VerificationStatus.VERIFIED);

		posting(101, 1, "VOLUNTEER", "OPEN", "주말 강아지 산책", List.of(ActivityCategory.WALK), "DOG",
				LocalDateTime.of(2026, 12, 10, 10, 0), 10, 2)
				.withPeriod(LocalDateTime.of(2026, 12, 8, 18, 0), null);
		posting(102, 1, "VOLUNTEER", "OPEN", "고양이 급식 도우미", List.of(ActivityCategory.FEEDING_CARE), "CAT",
				LocalDateTime.of(2026, 12, 20, 10, 0), 5, 5)
				.withUrgent(true, 50).withConditions(false, 18, "경험자")
				.withPeriod(LocalDateTime.of(2026, 12, 15, 18, 0), null);
		posting(103, 2, "SPONSOR", "OPEN", "겨울 담요 후원", List.of(ActivityCategory.ETC), "ALL", null, null, null)
				.withUrgent(true, 100).withPeriod(LocalDateTime.of(2026, 12, 5, 18, 0), null)
				.withSponsor("https://example.org/donate", "담요 구입");
		posting(104, 2, "FOSTER", "OPEN", "새끼 고양이 임시보호", List.of(ActivityCategory.FEEDING_CARE), "CAT",
				LocalDateTime.of(2026, 12, 12, 10, 0), 3, 0)
				.withUrgent(true, 70);
		posting(105, 3, "VOLUNTEER", "OPEN", "대형견 케어", List.of(ActivityCategory.FEEDING_CARE, ActivityCategory.WALK),
				"DOG", LocalDateTime.of(2026, 12, 10, 15, 0), 8, 1)
				.withPeriod(LocalDateTime.of(2026, 12, 9, 18, 0), null);
		posting(106, 4, "VOLUNTEER", "OPEN", "해변 산책 봉사", List.of(ActivityCategory.WALK), "DOG",
				LocalDateTime.of(2026, 12, 30, 10, 0), 8, 0);
		// 검색·긴급 조회에서 빠져야 하는 공고
		posting(107, 1, "VOLUNTEER", "CLOSED", "마감 공고", List.of(ActivityCategory.WALK), "DOG",
				LocalDateTime.of(2026, 12, 11, 10, 0), 5, 5).withUrgent(true, 999);
		posting(108, 1, "VOLUNTEER", "OPEN", "비공개 공고", List.of(ActivityCategory.WALK), "DOG",
				LocalDateTime.of(2026, 12, 11, 10, 0), 5, 0).withUrgent(true, 999).withPublic(false);
		savePostings();
	}

	/** 응답 items 의 postingId 를 순서대로 모은 문자열. 예: [106, 105] */
	List<Long> ids(ResultActions result, String path) throws Exception {
		String json = result.andReturn().getResponse().getContentAsString();
		List<Long> ids = new java.util.ArrayList<>();
		for (var node : objectMapper.readTree(json).at(path)) {
			ids.add(node.get("postingId").asLong());
		}
		return ids;
	}

	List<Long> search(String query) throws Exception {
		return ids(get("/api/v1/postings" + (query.isEmpty() ? "" : "?" + query)).andExpect(status().isOk()),
				"/data/items");
	}

	// ---- 기본 ----

	@Test
	void 모집_중인_공개_공고만_최신순으로_보여_준다() throws Exception {
		get("/api/v1/postings").andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.data.totalElements").value(6))
				.andExpect(jsonPath("$.data.page").value(0))
				.andExpect(jsonPath("$.data.size").value(20))
				.andExpect(jsonPath("$.data.totalPages").value(1))
				.andExpect(jsonPath("$.data.hasNext").value(false));
		assertThat(search("")).containsExactly(106L, 105L, 104L, 103L, 102L, 101L);
	}

	@Test
	void 항목에_공고_정보와_시설_요약이_담긴다() throws Exception {
		get("/api/v1/postings?keyword=주말").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items.length()").value(1))
				.andExpect(jsonPath("$.data.items[0].postingId").value(101))
				.andExpect(jsonPath("$.data.items[0].postingType").value("VOLUNTEER"))
				.andExpect(jsonPath("$.data.items[0].title").value("주말 강아지 산책"))
				.andExpect(jsonPath("$.data.items[0].status").value("OPEN"))
				.andExpect(jsonPath("$.data.items[0].urgent").value(false))
				.andExpect(jsonPath("$.data.items[0].shelter.shelterId").value(1))
				.andExpect(jsonPath("$.data.items[0].shelter.name").value("성북 행복보호소"))
				.andExpect(jsonPath("$.data.items[0].shelter.region.sido").value("서울특별시"))
				.andExpect(jsonPath("$.data.items[0].shelter.region.sigungu").value("성북구"))
				.andExpect(jsonPath("$.data.items[0].shelter.latitude").value(37.5894))
				.andExpect(jsonPath("$.data.items[0].shelter.longitude").value(127.0167))
				.andExpect(jsonPath("$.data.items[0].activityCategories[0]").value("WALK"))
				.andExpect(jsonPath("$.data.items[0].animalType").value("DOG"))
				.andExpect(jsonPath("$.data.items[0].activityStartAt").value("2026-12-10T10:00:00"))
				.andExpect(jsonPath("$.data.items[0].recruitEndAt").value("2026-12-08T18:00:00"))
				.andExpect(jsonPath("$.data.items[0].capacity").value(10))
				.andExpect(jsonPath("$.data.items[0].applicantCount").value(2))
				.andExpect(jsonPath("$.data.items[0].remainingCount").value(8))
				.andExpect(jsonPath("$.data.items[0].beginnerFriendly").value(true))
				.andExpect(jsonPath("$.data.items[0].distanceKm").isEmpty())
				.andExpect(jsonPath("$.data.items[0].bookmarked").value(false));
	}

	@Test
	void 모집_인원이_없으면_남은_자리는_null이고_가득_차면_0이다() throws Exception {
		get("/api/v1/postings?postingType=SPONSOR").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items[0].capacity").isEmpty())
				.andExpect(jsonPath("$.data.items[0].remainingCount").isEmpty());
		get("/api/v1/postings?keyword=급식").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items[0].remainingCount").value(0));
	}

	// ---- 필터 ----

	@Test
	void 공고_종류로_거른다() throws Exception {
		assertThat(search("postingType=SPONSOR")).containsExactly(103L);
		assertThat(search("postingType=FOSTER")).containsExactly(104L);
		assertThat(search("postingType=VOLUNTEER")).containsExactly(106L, 105L, 102L, 101L);
	}

	@Test
	void 활동_분류로_거르고_여러_개면_하나라도_겹치면_포함한다() throws Exception {
		assertThat(search("activityCategories=WALK")).containsExactly(106L, 105L, 101L);
		assertThat(search("activityCategories=FEEDING_CARE&activityCategories=ETC"))
				.containsExactly(105L, 104L, 103L, 102L);
		assertThat(search("activityCategories=FEEDING_CARE,ETC")).containsExactly(105L, 104L, 103L, 102L);
	}

	@Test
	void 동물_종류로_거르고_ALL_공고는_항상_포함한다() throws Exception {
		assertThat(search("animalType=DOG")).containsExactly(106L, 105L, 103L, 101L);
		assertThat(search("animalType=CAT")).containsExactly(104L, 103L, 102L);
	}

	@Test
	void 제목_검색어로_거른다() throws Exception {
		assertThat(search("keyword=산책")).containsExactly(106L, 101L);
	}

	@Test
	void 검색어가_시설_이름이나_지역에_있어도_찾는다() throws Exception {
		assertThat(search("keyword=부산")).containsExactly(106L);
		assertThat(search("keyword=해운대구")).containsExactly(106L);
		assertThat(search("keyword=행복보호소")).containsExactly(102L, 101L);
	}

	@Test
	void 검색어의_와일드카드_문자는_글자로_본다() throws Exception {
		posting(110, 1, "VOLUNTEER", "OPEN", "100% 봉사", List.of(ActivityCategory.ETC), "ALL", null, null, null);
		posting(111, 1, "VOLUNTEER", "OPEN", "100 봉사", List.of(ActivityCategory.ETC), "ALL", null, null, null);
		posting(112, 1, "VOLUNTEER", "OPEN", "a_b 봉사", List.of(ActivityCategory.ETC), "ALL", null, null, null);
		posting(113, 1, "VOLUNTEER", "OPEN", "axb 봉사", List.of(ActivityCategory.ETC), "ALL", null, null, null);
		savePostings();

		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/postings")
						.param("keyword", "%").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(1))
				.andExpect(jsonPath("$.data.items[0].postingId").value(110));
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/postings")
						.param("keyword", "a_b").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(1))
				.andExpect(jsonPath("$.data.items[0].postingId").value(112));
	}

	@Test
	void 지역으로_거른다() throws Exception {
		assertThat(search("sido=서울특별시")).containsExactly(104L, 103L, 102L, 101L);
		assertThat(search("sido=서울특별시&sigungu=노원구")).containsExactly(104L, 103L);
		assertThat(search("sido=제주특별자치도")).isEmpty();
	}

	@Test
	void 활동_시작일_범위로_거르고_일시_없는_공고는_뺀다() throws Exception {
		assertThat(search("startDate=2026-12-10&endDate=2026-12-10")).containsExactly(105L, 101L);
		assertThat(search("startDate=2026-12-20")).containsExactly(106L, 102L);
		assertThat(search("endDate=2026-12-11")).containsExactly(105L, 101L);
	}

	@Test
	void 경험_없이_참여할_수_있는_공고만_거른다() throws Exception {
		assertThat(search("beginnerFriendly=true")).containsExactly(106L, 105L, 104L, 103L, 101L);
		assertThat(search("beginnerFriendly=false")).hasSize(6);
	}

	@Test
	void 긴급_공고만_거른다() throws Exception {
		assertThat(search("urgentOnly=true")).containsExactly(104L, 103L, 102L);
		assertThat(search("urgentOnly=false")).hasSize(6);
	}

	@Test
	void 조건을_함께_쓰면_모두_만족하는_공고만_남는다() throws Exception {
		assertThat(search("postingType=VOLUNTEER&animalType=DOG&activityCategories=WALK&sido=서울특별시"))
				.containsExactly(101L);
	}

	// ---- 거리 ----

	@Test
	void 좌표를_보내면_거리를_함께_주고_거리순으로_정렬할_수_있다() throws Exception {
		get("/api/v1/postings?sort=DISTANCE&" + SEONGBUK).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items[0].postingId").value(102))
				.andExpect(jsonPath("$.data.items[0].distanceKm").value(0.0))
				.andExpect(jsonPath("$.data.items[2].postingId").value(104))
				.andExpect(jsonPath("$.data.items[2].distanceKm").value(org.hamcrest.Matchers.closeTo(8.0, 1.0)))
				.andExpect(jsonPath("$.data.items[5].postingId").value(106))
				.andExpect(jsonPath("$.data.items[5].distanceKm").value(org.hamcrest.Matchers.greaterThan(300.0)));
		assertThat(search("sort=DISTANCE&" + SEONGBUK)).containsExactly(102L, 101L, 104L, 103L, 105L, 106L);
	}

	@Test
	void 최대_거리_안의_공고만_보여_준다() throws Exception {
		assertThat(search("sort=DISTANCE&maxDistanceKm=10&" + SEONGBUK)).containsExactly(102L, 101L, 104L, 103L);
		assertThat(search("sort=DISTANCE&maxDistanceKm=25&" + SEONGBUK))
				.containsExactly(102L, 101L, 104L, 103L, 105L);
	}

	@Test
	void 좌표만_보내고_정렬을_안_하면_최신순을_지키고_거리만_채운다() throws Exception {
		assertThat(search(SEONGBUK)).containsExactly(106L, 105L, 104L, 103L, 102L, 101L);
		get("/api/v1/postings?keyword=주말&" + SEONGBUK).andExpect(jsonPath("$.data.items[0].distanceKm").value(0.0));
	}

	// ---- 정렬 ----

	@Test
	void 활동_임박순으로_정렬하고_일시_없는_공고는_뒤에_둔다() throws Exception {
		assertThat(search("sort=START_SOON")).containsExactly(101L, 105L, 104L, 102L, 106L, 103L);
	}

	@Test
	void 마감_임박순으로_정렬하고_마감_없는_공고는_뒤에_둔다() throws Exception {
		assertThat(search("sort=DEADLINE")).containsExactly(103L, 101L, 105L, 102L, 106L, 104L);
	}

	@Test
	void 긴급순은_긴급_공고를_우선순위대로_먼저_보여_준다() throws Exception {
		assertThat(search("sort=URGENT")).containsExactly(103L, 104L, 102L, 101L, 105L, 106L);
	}

	// ---- 페이지 ----

	@Test
	void 페이지로_나눠_준다() throws Exception {
		get("/api/v1/postings?page=0&size=4").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items.length()").value(4))
				.andExpect(jsonPath("$.data.totalElements").value(6))
				.andExpect(jsonPath("$.data.totalPages").value(2))
				.andExpect(jsonPath("$.data.hasNext").value(true));
		assertThat(search("page=1&size=4")).containsExactly(102L, 101L);
		assertThat(search("page=5&size=4")).isEmpty();
	}

	// ---- 관심 공고 ----

	@Test
	void 내_관심_공고만_bookmarked로_표시한다() throws Exception {
		bookmarkRepository.save(PostingBookmarkRef.of(user.getUserId(), 101L, LocalDateTime.of(2026, 11, 1, 9, 0)));
		bookmarkRepository.save(PostingBookmarkRef.of(other.getUserId(), 102L, LocalDateTime.of(2026, 11, 1, 9, 0)));

		get("/api/v1/postings?activityCategories=WALK&activityCategories=FEEDING_CARE&sort=START_SOON")
				.andExpect(jsonPath("$.data.items[0].postingId").value(101))
				.andExpect(jsonPath("$.data.items[0].bookmarked").value(true))
				.andExpect(jsonPath("$.data.items[1].postingId").value(105))
				.andExpect(jsonPath("$.data.items[1].bookmarked").value(false))
				.andExpect(jsonPath("$.data.items[4].postingId").value(106))
				.andExpect(jsonPath("$.data.items[?(@.postingId==102)].bookmarked").value(false));
	}

	// ---- 잘못된 요청 ----

	@Test
	void 조건_조합이_틀리면_400() throws Exception {
		for (String query : List.of("sigungu=성북구", "sido=", "keyword=", "latitude=37.5", "longitude=127.0",
				"maxDistanceKm=10", "sort=DISTANCE", SEONGBUK + "&maxDistanceKm=0", SEONGBUK + "&maxDistanceKm=201",
				"latitude=91&longitude=127", "latitude=37&longitude=181", "startDate=2026-12-20&endDate=2026-12-10")) {
			get("/api/v1/postings?" + query).andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.code").value("COMMON_400_2"));
		}
		get("/api/v1/postings?keyword=" + "가".repeat(51)).andExpect(status().isBadRequest());
	}

	@Test
	void 알_수_없는_값이면_400() throws Exception {
		for (String query : List.of("postingType=DONATION", "postingType=volunteer", "animalType=BIRD",
				"activityCategories=RUN", "sort=NEAREST", "page=-1", "size=0", "size=51")) {
			get("/api/v1/postings?" + query).andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.code").value("COMMON_400_2"));
		}
	}

	@Test
	void 형식이_틀리면_COMMON_400_1() throws Exception {
		for (String query : List.of("page=abc", "latitude=abc&longitude=127", "startDate=2026-13-40",
				"beginnerFriendly=maybe")) {
			get("/api/v1/postings?" + query).andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.code").value("COMMON_400_1"));
		}
	}

	@Test
	void 토큰이_없으면_401() throws Exception {
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/postings")).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("COMMON_401_1"));
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/postings/urgent")).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("COMMON_401_1"));
	}

	// ---- 긴급 공고 ----

	List<Long> urgent(String query) throws Exception {
		return ids(get("/api/v1/postings/urgent" + (query.isEmpty() ? "" : "?" + query)).andExpect(status().isOk()),
				"/data/items");
	}

	@Test
	void 긴급_공고를_우선순위대로_보여_주고_마감_공고와_비공개_공고는_뺀다() throws Exception {
		assertThat(urgent("")).containsExactly(103L, 104L, 102L);
		get("/api/v1/postings/urgent").andExpect(jsonPath("$.data.items[0].urgent").value(true))
				.andExpect(jsonPath("$.data.items[0].shelter.name").value("노원 온기쉼터"))
				.andExpect(jsonPath("$.data.page").doesNotExist());
	}

	@Test
	void 긴급_공고를_종류로_거르고_개수를_제한한다() throws Exception {
		assertThat(urgent("postingType=SPONSOR")).containsExactly(103L);
		assertThat(urgent("postingType=VOLUNTEER")).containsExactly(102L);
		assertThat(urgent("limit=2")).containsExactly(103L, 104L);
		assertThat(urgent("limit=1")).containsExactly(103L);
	}

	@Test
	void 긴급_공고의_우선순위가_같으면_마감이_빠른_순이다() throws Exception {
		posting(109, 1, "VOLUNTEER", "OPEN", "동점 A", List.of(ActivityCategory.WALK), "DOG", null, 5, 0)
				.withUrgent(true, 70).withPeriod(LocalDateTime.of(2026, 12, 1, 18, 0), null);
		savePostings();

		assertThat(urgent("")).containsExactly(103L, 109L, 104L, 102L);
	}

	@Test
	void 긴급_조회의_종류와_개수가_틀리면_400() throws Exception {
		for (String query : List.of("postingType=DONATION", "limit=0", "limit=21")) {
			get("/api/v1/postings/urgent?" + query).andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.code").value("COMMON_400_2"));
		}
		get("/api/v1/postings/urgent?limit=abc").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_400_1"));
	}

}
