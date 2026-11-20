package com.jjonggeut.domain.recommendation.repository;

import com.jjonggeut.domain.recommendation.entity.ShelterRef;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ShelterRefRepository extends JpaRepository<ShelterRef, Long>, JpaSpecificationExecutor<ShelterRef> {
}
