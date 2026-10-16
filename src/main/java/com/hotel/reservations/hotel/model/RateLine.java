package com.hotel.reservations.hotel.model;

import java.math.BigDecimal;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/** Nights of a stay that share the same season and therefore the same nightly rate. */
@Embeddable
public record RateLine(
        @Enumerated(EnumType.STRING) Season season,
        long nights,
        BigDecimal nightlyRate,
        BigDecimal subtotal) {
}
