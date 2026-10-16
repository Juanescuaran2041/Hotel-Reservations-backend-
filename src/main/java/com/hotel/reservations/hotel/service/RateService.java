package com.hotel.reservations.hotel.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.hotel.reservations.hotel.model.RateLine;
import com.hotel.reservations.hotel.model.RateQuote;
import com.hotel.reservations.hotel.model.RoomType;
import com.hotel.reservations.hotel.model.Season;
import com.hotel.reservations.hotel.pricing.PricingStrategy;
import com.hotel.reservations.hotel.pricing.PricingStrategyFactory;
import com.hotel.reservations.hotel.pricing.SeasonCalendar;

/** Subsystem: dynamic pricing by season, number of nights and room type. */
@Service
public class RateService {

    private final SeasonCalendar seasonCalendar;
    private final PricingStrategyFactory strategyFactory;

    public RateService(SeasonCalendar seasonCalendar, PricingStrategyFactory strategyFactory) {
        this.seasonCalendar = seasonCalendar;
        this.strategyFactory = strategyFactory;
    }

    /** Prices every night of the stay with the strategy of its season. */
    public RateQuote calculate(RoomType roomType, LocalDate checkIn, LocalDate checkOut) {
        Map<Season, Long> nightsPerSeason = new EnumMap<>(Season.class);
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            nightsPerSeason.merge(seasonCalendar.seasonOf(night), 1L, Long::sum);
        }

        List<RateLine> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        long nights = 0;
        for (Map.Entry<Season, Long> entry : nightsPerSeason.entrySet()) {
            PricingStrategy strategy = strategyFactory.forSeason(entry.getKey());
            BigDecimal nightlyRate = strategy.nightlyRate(roomType.getBasePrice());
            BigDecimal subtotal = nightlyRate.multiply(BigDecimal.valueOf(entry.getValue()));
            lines.add(new RateLine(entry.getKey(), entry.getValue(), nightlyRate, subtotal));
            total = total.add(subtotal);
            nights += entry.getValue();
        }
        return new RateQuote(roomType, nights, List.copyOf(lines), total);
    }
}
