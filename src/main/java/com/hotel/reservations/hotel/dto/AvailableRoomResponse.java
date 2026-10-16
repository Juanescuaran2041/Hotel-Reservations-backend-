package com.hotel.reservations.hotel.dto;

import java.math.BigDecimal;
import java.util.List;

import com.hotel.reservations.hotel.model.RoomType;

/** An available room together with the dynamic price preview for the requested stay. */
public record AvailableRoomResponse(
        String roomNumber,
        RoomType roomType,
        BigDecimal totalPrice,
        List<RateLineResponse> priceBreakdown) {
}
