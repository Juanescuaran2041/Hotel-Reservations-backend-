package com.hotel.reservations.hotel.model;

import java.math.BigDecimal;

/** An additional service attached to a reservation, with the price frozen when it was added. */
public record ServiceCharge(AdditionalServiceType type, int quantity, BigDecimal unitPrice, BigDecimal total) {
}
