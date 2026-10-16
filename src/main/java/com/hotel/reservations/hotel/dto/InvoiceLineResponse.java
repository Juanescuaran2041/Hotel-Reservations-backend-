package com.hotel.reservations.hotel.dto;

import java.math.BigDecimal;

public record InvoiceLineResponse(String description, long quantity, BigDecimal unitPrice, BigDecimal amount) {
}
