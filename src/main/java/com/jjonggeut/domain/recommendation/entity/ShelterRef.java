package com.jjonggeut.domain.recommendation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * 추천이 읽는 보호시설(shelter) 테이블의 읽기 전용 매핑.
 * NOTE: 시설 담당 팀의 실제 테이블이 확정되면 이 클래스의 @Table/@Column 이름만 맞춘다.
 */
@Entity
@Immutable
@Table(name = "shelter")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShelterRef {

	@Id
	@Column(name = "shelter_id")
	private Long shelterId;

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	@Column(name = "region_sido", length = 50)
	private String regionSido;

	@Column(name = "region_sigungu", length = 50)
	private String regionSigungu;

	@Column(name = "latitude")
	private Double latitude;

	@Column(name = "longitude")
	private Double longitude;

	public static ShelterRef of(Long shelterId, String name, String regionSido, String regionSigungu,
			Double latitude, Double longitude) {
		ShelterRef shelter = new ShelterRef();
		shelter.shelterId = shelterId;
		shelter.name = name;
		shelter.regionSido = regionSido;
		shelter.regionSigungu = regionSigungu;
		shelter.latitude = latitude;
		shelter.longitude = longitude;
		return shelter;
	}
}
