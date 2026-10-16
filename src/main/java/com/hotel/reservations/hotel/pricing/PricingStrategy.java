package com.hotel.reservations.hotel.pricing;

import java.math.BigDecimal;

import com.hotel.reservations.hotel.model.Season;

/** Strategy pattern: each season knows how to turn a base price into a nightly rate. */
public interface PricingStrategy {

    Season season();

    BigDecimal nightlyRate(BigDecimal basePrice);
}
