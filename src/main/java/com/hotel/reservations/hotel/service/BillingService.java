package com.hotel.reservations.hotel.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.hotel.reservations.hotel.model.Invoice;
import com.hotel.reservations.hotel.model.InvoiceLine;
import com.hotel.reservations.hotel.model.RateLine;
import com.hotel.reservations.hotel.model.Reservation;
import com.hotel.reservations.hotel.model.ServiceCharge;
import com.hotel.reservations.hotel.repository.InvoiceRepository;
import com.hotel.reservations.hotel.util.InvoiceNumberGenerator;

/** Subsystem: builds the itemized final invoice. */
@Service
public class BillingService {

    private final InvoiceRepository invoiceRepository;

    public BillingService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    public Invoice generateInvoice(Reservation reservation) {
        String roomNumber = reservation.getRoom().getNumber();
        Invoice.Builder builder = Invoice.builder()
                .number(InvoiceNumberGenerator.getInstance().next())
                .reservationId(reservation.getId())
                .guest(reservation.getGuest())
                .room(roomNumber)
                .stay(reservation.getCheckInDate(), reservation.getCheckOutDate())
                .issuedAt(Instant.now());

        for (RateLine line : reservation.getRateQuote().lines()) {
            builder.addRoomLine(new InvoiceLine(
                    "Room %s (%s) - %s season".formatted(roomNumber, reservation.getRateQuote().roomType(), line.season()),
                    line.nights(), line.nightlyRate(), line.subtotal()));
        }
        for (ServiceCharge charge : reservation.getServices()) {
            builder.addServiceLine(new InvoiceLine(
                    charge.type().getDisplayName(), charge.quantity(), charge.unitPrice(), charge.total()));
        }
        return invoiceRepository.save(builder.build());
    }

    public Optional<Invoice> findByReservationId(UUID reservationId) {
        return invoiceRepository.findById(reservationId);
    }
}
