package com.jjonggeut.domain.posting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jjonggeut.domain.activity.entity.PostingBookmarkRef;
import com.jjonggeut.domain.activity.repository.PostingBookmarkRefRepository;
import org.springframework.test.web.servlet.ResultActions;
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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PostingBookmarkTest extends PostingTestBase {

	@Autowired
	PostingBookmarkRefRepository bookmarkRepository;

	@BeforeEach
	void setUpData() {
		shelter(1, "성북 행복보호소", "서울특별시", "성북구", 37.5894, 127.0167, VerificationStatus.VERIFIED);
		posting(301, 1, "VOLUNTEER", "OPEN", "산책 봉사", List.of(ActivityCategory.WALK), "DOG", null, 5, 0);
		posting(302, 1, "VOLUNTEER", "OPEN", "비공개 공고", List.of(ActivityCategory.WALK), "DOG", null, 5, 0)
				.withPublic(false);
		posting(303, 1, "VOLUNTEER", "CLOSED", "마감 공고", List.of(ActivityCategory.WALK), "DOG", null, 5, 5);
		savePostings();
	}

	private ResultActions save(Object postingId) throws Exception {
		return send(post("/api/v1/postings/" + postingId + "/bookmarks"), null);
	}

	private ResultActions remove(Object postingId) throws Exception {
		return send(delete("/api/v1/postings/" + postingId + "/bookmarks"), null);
	}

	@Test
	void 관심_공고로_저장한다() throws Exception {
		save(301).andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.data.postingId").value(301))
				.andExpect(jsonPath("$.data.bookmarked").value(true))
				.andExpect(jsonPath("$.data.createdAt").isNotEmpty());

		assertThat(bookmarkRepository.existsByUserIdAndPostingId(user.getUserId(), 301L)).isTrue();
		get("/api/v1/postings/301").andExpect(jsonPath("$.data.bookmarked").value(true));
	}

	@Test
	void 이미_저장한_공고면_409이고_한_건만_남는다() throws Exception {
		save(301).andExpect(status().isOk());

		save(301).andExpect(status().isConflict())
				.andExpect(jsonPath("$.isSuccess").value(false))
				.andExpect(jsonPath("$.code").value("POSTING_409_1"));
		assertThat(bookmarkRepository.findAll().stream()
				.filter(b -> b.getUserId().equals(user.getUserId())).count()).isEqualTo(1);
	}

	@Test
	void 다른_사용자가_저장한_공고도_내가_저장할_수_있다() throws Exception {
		bookmarkRepository.save(PostingBookmarkRef.of(other.getUserId(), 301L, LocalDateTime.of(2026, 11, 1, 9, 0)));

		save(301).andExpect(status().isOk());
		assertThat(bookmarkRepository.findAll()).hasSize(2);
	}

	@Test
	void 모집이_끝난_공고도_저장할_수_있다() throws Exception {
		save(303).andExpect(status().isOk());
	}

	@Test
	void 저장한_공고를_해제한다() throws Exception {
		save(301).andExpect(status().isOk());

		remove(301).andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.code").value("COMMON_200"));
		assertThat(bookmarkRepository.existsByUserIdAndPostingId(user.getUserId(), 301L)).isFalse();
		get("/api/v1/postings/301").andExpect(jsonPath("$.data.bookmarked").value(false));
	}

	@Test
	void 해제한_뒤에는_다시_저장할_수_있다() throws Exception {
		save(301).andExpect(status().isOk());
		remove(301).andExpect(status().isOk());

		save(301).andExpect(status().isOk());
	}

	@Test
	void 저장하지_않은_공고를_해제하면_404() throws Exception {
		remove(301).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("POSTING_404_2"));
	}

	@Test
	void 해제는_내_저장만_지운다() throws Exception {
		bookmarkRepository.save(PostingBookmarkRef.of(other.getUserId(), 301L, LocalDateTime.of(2026, 11, 1, 9, 0)));

		remove(301).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("POSTING_404_2"));
		assertThat(bookmarkRepository.existsByUserIdAndPostingId(other.getUserId(), 301L)).isTrue();
	}

	@Test
	void 없거나_비공개인_공고는_저장도_해제도_404() throws Exception {
		save(9999).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("POSTING_404_1"));
		save(302).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("POSTING_404_1"));
		remove(9999).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("POSTING_404_1"));
	}

	@Test
	void 공고_ID가_1_미만이면_400이고_숫자가_아니면_COMMON_400_1() throws Exception {
		save(0).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
		remove(-1).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_2"));
		save("abc").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMON_400_1"));
	}

	@Test
	void 토큰이_없으면_401() throws Exception {
		mockMvc.perform(post("/api/v1/postings/301/bookmarks")).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("COMMON_401_1"));
		mockMvc.perform(delete("/api/v1/postings/301/bookmarks")).andExpect(status().isUnauthorized());
	}
}
