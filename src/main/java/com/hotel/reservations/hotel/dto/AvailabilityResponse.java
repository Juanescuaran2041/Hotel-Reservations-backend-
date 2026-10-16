package com.hotel.reservations.hotel.dto;

import java.time.LocalDate;
import java.util.List;

public record AvailabilityResponse(
        LocalDate checkInDate,
        LocalDate checkOutDate,
        long nights,
        List<AvailableRoomResponse> rooms) {
}
