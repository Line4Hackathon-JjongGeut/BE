package com.jjonggeut.domain.posting.repository;

import com.jjonggeut.domain.posting.entity.PostingApplicationQuestion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostingApplicationQuestionRepository extends JpaRepository<PostingApplicationQuestion, Long> {

	List<PostingApplicationQuestion> findByPostingIdOrderBySortOrderAscQuestionIdAsc(Long postingId);
}
