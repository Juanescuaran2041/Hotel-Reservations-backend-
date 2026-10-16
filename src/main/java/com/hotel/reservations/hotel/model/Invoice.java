package com.hotel.reservations.hotel.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Immutable final bill. Created through {@link Builder} (Builder pattern). */
public final class Invoice {

    private final String number;
    private final UUID reservationId;
    private final Guest guest;
    private final String roomNumber;
    private final LocalDate checkInDate;
    private final LocalDate checkOutDate;
    private final List<InvoiceLine> roomLines;
    private final List<InvoiceLine> serviceLines;
    private final BigDecimal roomSubtotal;
    private final BigDecimal servicesSubtotal;
    private final BigDecimal total;
    private final Instant issuedAt;

    private Invoice(Builder builder) {
        this.number = builder.number;
        this.reservationId = builder.reservationId;
        this.guest = builder.guest;
        this.roomNumber = builder.roomNumber;
        this.checkInDate = builder.checkInDate;
        this.checkOutDate = builder.checkOutDate;
        this.roomLines = List.copyOf(builder.roomLines);
        this.serviceLines = List.copyOf(builder.serviceLines);
        this.roomSubtotal = sum(roomLines);
        this.servicesSubtotal = sum(serviceLines);
        this.total = roomSubtotal.add(servicesSubtotal);
        this.issuedAt = builder.issuedAt;
    }

    private static BigDecimal sum(List<InvoiceLine> lines) {
        return lines.stream().map(InvoiceLine::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getNumber() {
        return number;
    }

    public UUID getReservationId() {
        return reservationId;
    }

    public Guest getGuest() {
        return guest;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }

    public List<InvoiceLine> getRoomLines() {
        return roomLines;
    }

    public List<InvoiceLine> getServiceLines() {
        return serviceLines;
    }

    public BigDecimal getRoomSubtotal() {
        return roomSubtotal;
    }

    public BigDecimal getServicesSubtotal() {
        return servicesSubtotal;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public static final class Builder {
        private String number;
        private UUID reservationId;
        private Guest guest;
        private String roomNumber;
        private LocalDate checkInDate;
        private LocalDate checkOutDate;
        private final List<InvoiceLine> roomLines = new ArrayList<>();
        private final List<InvoiceLine> serviceLines = new ArrayList<>();
        private Instant issuedAt = Instant.now();

        private Builder() {
        }

        public Builder number(String number) {
            this.number = number;
            return this;
        }

        public Builder reservationId(UUID reservationId) {
            this.reservationId = reservationId;
            return this;
        }

        public Builder guest(Guest guest) {
            this.guest = guest;
            return this;
        }

        public Builder room(String roomNumber) {
            this.roomNumber = roomNumber;
            return this;
        }

        public Builder stay(LocalDate checkInDate, LocalDate checkOutDate) {
            this.checkInDate = checkInDate;
            this.checkOutDate = checkOutDate;
            return this;
        }

        public Builder addRoomLine(InvoiceLine line) {
            this.roomLines.add(line);
            return this;
        }

        public Builder addServiceLine(InvoiceLine line) {
            this.serviceLines.add(line);
            return this;
        }

        public Builder issuedAt(Instant issuedAt) {
            this.issuedAt = issuedAt;
            return this;
        }

        public Invoice build() {
            if (number == null || reservationId == null || guest == null || roomNumber == null
                    || checkInDate == null || checkOutDate == null) {
                throw new IllegalStateException("Invoice is missing mandatory data");
            }
            return new Invoice(this);
        }
    }
}
