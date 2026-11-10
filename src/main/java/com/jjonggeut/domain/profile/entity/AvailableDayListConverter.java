package com.jjonggeut.domain.profile.entity;

import jakarta.persistence.Converter;

@Converter
public class AvailableDayListConverter extends EnumListConverter<AvailableDay> {

	public AvailableDayListConverter() {
		super(AvailableDay.class);
	}
}
