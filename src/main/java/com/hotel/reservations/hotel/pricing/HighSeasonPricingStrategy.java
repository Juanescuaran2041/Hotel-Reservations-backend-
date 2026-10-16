package com.hotel.reservations.hotel.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

import com.hotel.reservations.hotel.model.Season;

/** High season: base price x 1.5. */
@Component
public class HighSeasonPricingStrategy implements PricingStrategy {

    private static final BigDecimal MULTIPLIER = new BigDecimal("1.5");

    @Override
    public Season season() {
        return Season.HIGH;
    }

    @Override
    public BigDecimal nightlyRate(BigDecimal basePrice) {
        return basePrice.multiply(MULTIPLIER).setScale(2, RoundingMode.HALF_UP);
    }
}
