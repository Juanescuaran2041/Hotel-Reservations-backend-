package com.hotel.reservations.hotel.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/** An additional service attached to a reservation, with the price frozen when it was added. */
@Embeddable
public record ServiceCharge(
        @Enumerated(EnumType.STRING) @Column(name = "service_type") AdditionalServiceType type,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal total) {
}
