package com.hotel.reservations.hotel.model;

import java.math.BigDecimal;

/** Nights of a stay that share the same season and therefore the same nightly rate. */
public record RateLine(Season season, long nights, BigDecimal nightlyRate, BigDecimal subtotal) {
}
