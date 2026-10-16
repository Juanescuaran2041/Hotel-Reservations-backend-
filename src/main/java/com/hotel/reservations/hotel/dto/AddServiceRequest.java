package com.hotel.reservations.hotel.dto;

import com.hotel.reservations.hotel.model.AdditionalServiceType;

import jakarta.validation.constraints.NotNull;

public record AddServiceRequest(@NotNull AdditionalServiceType serviceType) {
}
