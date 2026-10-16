package com.hotel.reservations.hotel.dto;

import java.math.BigDecimal;

import com.hotel.reservations.hotel.model.AdditionalServiceType;

public record ServiceChargeResponse(
        AdditionalServiceType serviceType,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal total) {
}
