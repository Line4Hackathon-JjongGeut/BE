package com.jjonggeut.domain.recommendation.entity;

import com.jjonggeut.domain.profile.entity.InterestAnimal;
import com.jjonggeut.domain.profile.entity.InterestAnimalListConverter;
import com.jjonggeut.domain.shelter.entity.VerificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * 보호시설(shelter) 테이블의 읽기 전용 매핑. 추천과 보호시설 조회가 함께 쓴다.
 * address, phone, description, animalTypes, verificationStatus 는 값이 없는 행도 있을 수 있다.
 * NOTE: 실제 테이블이 확정되면 이 클래스의 @Table/@Column 이름만 맞춘다.
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

	@Column(name = "address", length = 255)
	private String address;

	@Column(name = "phone", length = 20)
	private String phone;

	@Column(name = "description", length = 2000)
	private String description;

	/** 보호하는 동물 유형. 값이 없으면 빈 목록이다. */
	@Convert(converter = InterestAnimalListConverter.class)
	@Column(name = "animal_types", length = 255)
	private List<InterestAnimal> animalTypes;

	/** 값이 없으면 미인증(UNVERIFIED)으로 본다. */
	@Enumerated(EnumType.STRING)
	@Column(name = "verification_status", length = 20)
	private VerificationStatus verificationStatus;

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

	public static ShelterRef of(Long shelterId, String name, String regionSido, String regionSigungu,
			String address, Double latitude, Double longitude, String phone, String description,
			List<InterestAnimal> animalTypes, VerificationStatus verificationStatus) {
		ShelterRef shelter = of(shelterId, name, regionSido, regionSigungu, latitude, longitude);
		shelter.address = address;
		shelter.phone = phone;
		shelter.description = description;
		shelter.animalTypes = animalTypes;
		shelter.verificationStatus = verificationStatus;
		return shelter;
	}

	public List<InterestAnimal> getAnimalTypes() {
		return animalTypes == null ? List.of() : animalTypes;
	}

	public VerificationStatus getVerificationStatus() {
		return verificationStatus == null ? VerificationStatus.UNVERIFIED : verificationStatus;
	}
}
