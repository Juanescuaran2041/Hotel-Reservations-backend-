package com.hotel.reservations.hotel.facade;

import java.util.List;

import org.springframework.stereotype.Component;

import com.hotel.reservations.hotel.dto.AvailableRoomResponse;
import com.hotel.reservations.hotel.dto.DigitalKeyResponse;
import com.hotel.reservations.hotel.dto.InvoiceLineResponse;
import com.hotel.reservations.hotel.dto.InvoiceResponse;
import com.hotel.reservations.hotel.dto.RateLineResponse;
import com.hotel.reservations.hotel.dto.ReservationResponse;
import com.hotel.reservations.hotel.dto.ServiceChargeResponse;
import com.hotel.reservations.hotel.model.DigitalKey;
import com.hotel.reservations.hotel.model.Invoice;
import com.hotel.reservations.hotel.model.InvoiceLine;
import com.hotel.reservations.hotel.model.RateLine;
import com.hotel.reservations.hotel.model.RateQuote;
import com.hotel.reservations.hotel.model.Reservation;
import com.hotel.reservations.hotel.model.Room;
import com.hotel.reservations.hotel.model.ServiceCharge;

/** Converts domain objects into the DTOs exposed by the API. */
@Component
public class HotelMapper {

    public ReservationResponse toResponse(Reservation reservation, Invoice invoice) {
        RateQuote quote = reservation.getRateQuote();
        return new ReservationResponse(
                reservation.getId(),
                reservation.getStatus(),
                reservation.getGuest().fullName(),
                reservation.getGuest().email(),
                reservation.getGuest().phone(),
                reservation.getRoom().getNumber(),
                reservation.getRoom().getType(),
                reservation.getCheckInDate(),
                reservation.getCheckOutDate(),
                reservation.getNights(),
                toRateLines(quote.lines()),
                reservation.getRoomCost(),
                reservation.getServices().stream().map(this::toServiceResponse).toList(),
                reservation.getServicesTotal(),
                reservation.getRunningTotal(),
                toKeyResponse(reservation.getDigitalKey()),
                invoice == null ? null : toInvoiceResponse(invoice));
    }

    public AvailableRoomResponse toAvailableRoom(Room room, RateQuote quote) {
        return new AvailableRoomResponse(room.getNumber(), room.getType(), quote.total(), toRateLines(quote.lines()));
    }

    public InvoiceResponse toInvoiceResponse(Invoice invoice) {
        return new InvoiceResponse(
                invoice.getNumber(),
                invoice.getReservationId(),
                invoice.getGuest().fullName(),
                invoice.getGuest().email(),
                invoice.getRoomNumber(),
                invoice.getCheckInDate(),
                invoice.getCheckOutDate(),
                invoice.getRoomLines().stream().map(this::toInvoiceLine).toList(),
                invoice.getServiceLines().stream().map(this::toInvoiceLine).toList(),
                invoice.getRoomSubtotal(),
                invoice.getServicesSubtotal(),
                invoice.getTotal(),
                invoice.getIssuedAt());
    }

    private List<RateLineResponse> toRateLines(List<RateLine> lines) {
        return lines.stream()
                .map(line -> new RateLineResponse(line.season(), line.nights(), line.nightlyRate(), line.subtotal()))
                .toList();
    }

    private ServiceChargeResponse toServiceResponse(ServiceCharge charge) {
        return new ServiceChargeResponse(charge.type(), charge.quantity(), charge.unitPrice(), charge.total());
    }

    private DigitalKeyResponse toKeyResponse(DigitalKey key) {
        return key == null ? null : new DigitalKeyResponse(key.code(), key.roomNumber(), key.issuedAt());
    }

    private InvoiceLineResponse toInvoiceLine(InvoiceLine line) {
        return new InvoiceLineResponse(line.description(), line.quantity(), line.unitPrice(), line.amount());
    }
}
