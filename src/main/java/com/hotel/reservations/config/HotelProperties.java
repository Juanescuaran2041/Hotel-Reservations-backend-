package com.hotel.reservations.config;

import java.time.ZoneId;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Hotel business settings.
 *
 * @param highSeasonMonths months (1-12) that are priced as high season
 * @param timezone time zone of the hotel; decides what "today" means when validating check-in dates
 */
@ConfigurationProperties(prefix = "hotel")
public record HotelProperties(Set<Integer> highSeasonMonths, ZoneId timezone) {

    public HotelProperties {
        highSeasonMonths = highSeasonMonths == null ? Set.of(6, 7, 12, 1) : Set.copyOf(highSeasonMonths);
        timezone = timezone == null ? ZoneId.of("America/Bogota") : timezone;
    }
}
