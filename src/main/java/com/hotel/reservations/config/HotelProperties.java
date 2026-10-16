package com.hotel.reservations.config;

import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Hotel business settings.
 *
 * @param highSeasonMonths months (1-12) that are priced as high season
 */
@ConfigurationProperties(prefix = "hotel")
public record HotelProperties(Set<Integer> highSeasonMonths) {

    public HotelProperties {
        highSeasonMonths = highSeasonMonths == null ? Set.of(6, 7, 12, 1) : Set.copyOf(highSeasonMonths);
    }
}
