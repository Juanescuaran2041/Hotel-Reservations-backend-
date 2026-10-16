package com.hotel.reservations.hotel.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

import com.hotel.reservations.hotel.model.Season;

/** Low season: the base price. */
@Component
public class LowSeasonPricingStrategy implements PricingStrategy {

    @Override
    public Season season() {
        return Season.LOW;
    }

    @Override
    public BigDecimal nightlyRate(BigDecimal basePrice) {
        return basePrice.setScale(2, RoundingMode.HALF_UP);
    }
}
