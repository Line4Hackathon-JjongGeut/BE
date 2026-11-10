package com.jjonggeut.domain.recommendation.repository;

import com.jjonggeut.domain.recommendation.entity.UserActivityEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserActivityEventRepository extends JpaRepository<UserActivityEvent, Long> {
}
