package com.jjonggeut.domain.profile.entity;

import jakarta.persistence.Converter;

@Converter
public class InterestAnimalListConverter extends EnumListConverter<InterestAnimal> {

	public InterestAnimalListConverter() {
		super(InterestAnimal.class);
	}
}
