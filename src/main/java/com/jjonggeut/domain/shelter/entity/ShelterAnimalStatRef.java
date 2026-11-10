package com.jjonggeut.domain.shelter.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * 시설별 보호동물 현황(shelter_animal_stat)의 읽기 전용 매핑. 시설당 1행이며 갱신은 운영자 기능의 몫이다.
 * of() 는 테스트와 임시 데이터용이다.
 */
@Entity
@Immutable
@Table(name = "shelter_animal_stat")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShelterAnimalStatRef {

	@Id
	@Column(name = "shelter_id")
	private Long shelterId;

	@Column(name = "dog_count", nullable = false)
	private int dogCount;

	@Column(name = "cat_count", nullable = false)
	private int catCount;

	@Column(name = "other_count", nullable = false)
	private int otherCount;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	public static ShelterAnimalStatRef of(Long shelterId, int dogCount, int catCount, int otherCount,
			LocalDateTime updatedAt) {
		ShelterAnimalStatRef stat = new ShelterAnimalStatRef();
		stat.shelterId = shelterId;
		stat.dogCount = dogCount;
		stat.catCount = catCount;
		stat.otherCount = otherCount;
		stat.updatedAt = updatedAt;
		return stat;
	}
}
