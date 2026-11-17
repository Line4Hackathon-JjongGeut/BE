package com.jjonggeut.domain.posting.repository;

import com.jjonggeut.domain.posting.entity.PostingMedia;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostingMediaRepository extends JpaRepository<PostingMedia, Long> {

	List<PostingMedia> findByPostingIdInOrderByPostingIdAscPositionAsc(Collection<Long> postingIds);
}
