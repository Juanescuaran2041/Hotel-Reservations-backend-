package com.hotel.reservations.hotel.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.hotel.reservations.hotel.model.ReservationStatus;
import com.hotel.reservations.hotel.model.RoomType;

/**
 * Full view of a reservation. {@code digitalKey} is only present while the guest is checked in and
 * {@code invoice} only after check-out.
 */
public record ReservationResponse(
        UUID id,
        ReservationStatus status,
        String guestName,
        String guestEmail,
        String guestPhone,
        String roomNumber,
        RoomType roomType,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        long nights,
        List<RateLineResponse> priceBreakdown,
        BigDecimal roomCost,
        List<ServiceChargeResponse> services,
        BigDecimal servicesTotal,
        BigDecimal total,
        DigitalKeyResponse digitalKey,
        InvoiceResponse invoice) {
}
