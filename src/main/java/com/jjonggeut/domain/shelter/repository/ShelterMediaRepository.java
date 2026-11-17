package com.jjonggeut.domain.shelter.repository;

import com.jjonggeut.domain.shelter.entity.ShelterMedia;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShelterMediaRepository extends JpaRepository<ShelterMedia, Long> {

	List<ShelterMedia> findByShelterIdInOrderByShelterIdAscPositionAsc(Collection<Long> shelterIds);
}
