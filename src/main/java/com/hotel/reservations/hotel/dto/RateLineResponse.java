package com.hotel.reservations.hotel.dto;

import java.math.BigDecimal;

import com.hotel.reservations.hotel.model.Season;

public record RateLineResponse(Season season, long nights, BigDecimal nightlyRate, BigDecimal subtotal) {
}
