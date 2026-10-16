package com.hotel.reservations.hotel.model;

import java.math.BigDecimal;

import jakarta.persistence.Embeddable;

@Embeddable
public record InvoiceLine(String description, long quantity, BigDecimal unitPrice, BigDecimal amount) {
}
