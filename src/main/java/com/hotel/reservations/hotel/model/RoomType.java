package com.hotel.reservations.hotel.model;

import java.math.BigDecimal;

public enum RoomType {
    SINGLE(new BigDecimal("80.00")),
    DOUBLE(new BigDecimal("120.00")),
    SUITE(new BigDecimal("250.00"));

    private final BigDecimal basePrice;

    RoomType(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }
}
