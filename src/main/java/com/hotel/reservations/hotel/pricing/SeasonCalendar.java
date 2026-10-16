package com.hotel.reservations.hotel.pricing;

import java.time.LocalDate;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.hotel.reservations.config.HotelProperties;
import com.hotel.reservations.hotel.model.Season;

/** Decides which season a given night belongs to. */
@Component
public class SeasonCalendar {

    private final Set<Integer> highSeasonMonths;

    public SeasonCalendar(HotelProperties properties) {
        this.highSeasonMonths = properties.highSeasonMonths();
    }

    public Season seasonOf(LocalDate night) {
        return highSeasonMonths.contains(night.getMonthValue()) ? Season.HIGH : Season.LOW;
    }
}
