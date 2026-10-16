package com.hotel.reservations.hotel.model;

import java.math.BigDecimal;

public record InvoiceLine(String description, long quantity, BigDecimal unitPrice, BigDecimal amount) {
}
