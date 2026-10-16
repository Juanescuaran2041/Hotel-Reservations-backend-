package com.hotel.reservations.hotel.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InvoiceResponse(
        String invoiceNumber,
        UUID reservationId,
        String guestName,
        String guestEmail,
        String roomNumber,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        List<InvoiceLineResponse> roomLines,
        List<InvoiceLineResponse> serviceLines,
        BigDecimal roomSubtotal,
        BigDecimal servicesSubtotal,
        BigDecimal total,
        Instant issuedAt) {
}
