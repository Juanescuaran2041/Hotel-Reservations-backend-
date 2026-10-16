package com.hotel.reservations.hotel;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.hotel.reservations.config.HotelProperties;
import com.hotel.reservations.hotel.model.RateQuote;
import com.hotel.reservations.hotel.model.RoomType;
import com.hotel.reservations.hotel.model.Season;
import com.hotel.reservations.hotel.pricing.HighSeasonPricingStrategy;
import com.hotel.reservations.hotel.pricing.LowSeasonPricingStrategy;
import com.hotel.reservations.hotel.pricing.PricingStrategyFactory;
import com.hotel.reservations.hotel.pricing.SeasonCalendar;
import com.hotel.reservations.hotel.service.RateService;
import com.hotel.reservations.hotel.util.InvoiceNumberGenerator;

class RateServiceTest {

    private final RateService rateService = new RateService(
            new SeasonCalendar(new HotelProperties(null)),
            new PricingStrategyFactory(List.of(new HighSeasonPricingStrategy(), new LowSeasonPricingStrategy())));

    @Test
    void lowSeasonUsesBasePrice() {
        RateQuote quote = rateService.calculate(RoomType.DOUBLE, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 13));

        assertThat(quote.nights()).isEqualTo(3);
        assertThat(quote.total()).isEqualByComparingTo("360.00");
    }

    @Test
    void highSeasonAppliesOnePointFiveMultiplier() {
        RateQuote quote = rateService.calculate(RoomType.SUITE, LocalDate.of(2027, 7, 1), LocalDate.of(2027, 7, 3));

        assertThat(quote.total()).isEqualByComparingTo("750.00");
        assertThat(quote.lines()).singleElement().satisfies(line -> assertThat(line.season()).isEqualTo(Season.HIGH));
    }

    @Test
    void staySpanningSeasonsIsPricedNightByNight() {
        RateQuote quote = rateService.calculate(RoomType.DOUBLE, LocalDate.of(2027, 11, 29), LocalDate.of(2027, 12, 2));

        assertThat(quote.lines()).hasSize(2);
        assertThat(quote.total()).isEqualByComparingTo(new BigDecimal("420.00"));
    }

    @Test
    void invoiceNumberGeneratorIsASingleton() {
        assertThat(InvoiceNumberGenerator.getInstance()).isSameAs(InvoiceNumberGenerator.getInstance());
        assertThat(InvoiceNumberGenerator.getInstance().next())
                .isNotEqualTo(InvoiceNumberGenerator.getInstance().next());
    }
}
