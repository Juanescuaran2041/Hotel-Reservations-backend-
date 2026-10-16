package com.hotel.reservations.hotel.pricing;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.hotel.reservations.hotel.model.Season;

/** Factory that selects the pricing strategy registered for a season. */
@Component
public class PricingStrategyFactory {

    private final Map<Season, PricingStrategy> strategies = new EnumMap<>(Season.class);

    public PricingStrategyFactory(List<PricingStrategy> availableStrategies) {
        availableStrategies.forEach(strategy -> strategies.put(strategy.season(), strategy));
        for (Season season : Season.values()) {
            if (!strategies.containsKey(season)) {
                throw new IllegalStateException("No pricing strategy registered for season " + season);
            }
        }
    }

    public PricingStrategy forSeason(Season season) {
        return strategies.get(season);
    }
}
