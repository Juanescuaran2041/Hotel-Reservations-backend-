package com.hotel.reservations.hotel.model;

import java.time.LocalDate;
import java.util.UUID;

/** A date range during which a room is held by a reservation. The check-out date is exclusive. */
public record Booking(UUID reservationId, LocalDate checkIn, LocalDate checkOut) {

    public boolean overlaps(LocalDate from, LocalDate to) {
        return checkIn.isBefore(to) && from.isBefore(checkOut);
    }
}
