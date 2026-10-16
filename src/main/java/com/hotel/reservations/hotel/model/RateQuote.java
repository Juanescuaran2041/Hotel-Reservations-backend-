package com.hotel.reservations.hotel.model;

import java.math.BigDecimal;
import java.util.List;

/** Result of pricing a stay: the breakdown per season and the grand total of the room. */
public record RateQuote(RoomType roomType, long nights, List<RateLine> lines, BigDecimal total) {
}
