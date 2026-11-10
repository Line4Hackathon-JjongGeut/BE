package com.jjonggeut.domain.profile.entity;

import jakarta.persistence.Converter;

@Converter
public class AvailableTimeSlotListConverter extends EnumListConverter<AvailableTimeSlot> {

	public AvailableTimeSlotListConverter() {
		super(AvailableTimeSlot.class);
	}
}
