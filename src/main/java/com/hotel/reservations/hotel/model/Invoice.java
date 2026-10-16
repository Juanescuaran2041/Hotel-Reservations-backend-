package com.hotel.reservations.hotel.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

/**
 * Final bill, never modified after it is issued. Created through {@link Builder} (Builder pattern).
 * There is one invoice per reservation, so the reservation id is also its key.
 */
@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @Column(name = "reservation_id")
    private UUID reservationId;

    @Column(nullable = false, unique = true, length = 20)
    private String number;

    @Embedded
    private Guest guest;

    @Column(name = "room_number", nullable = false)
    private String roomNumber;

    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "check_out_date", nullable = false)
    private LocalDate checkOutDate;

    @ElementCollection
    @CollectionTable(name = "invoice_room_lines", joinColumns = @JoinColumn(name = "invoice_id"))
    @OrderColumn(name = "line_order")
    private List<InvoiceLine> roomLines = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "invoice_service_lines", joinColumns = @JoinColumn(name = "invoice_id"))
    @OrderColumn(name = "line_order")
    private List<InvoiceLine> serviceLines = new ArrayList<>();

    @Column(name = "room_subtotal", nullable = false)
    private BigDecimal roomSubtotal;

    @Column(name = "services_subtotal", nullable = false)
    private BigDecimal servicesSubtotal;

    @Column(nullable = false)
    private BigDecimal total;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    protected Invoice() {
    }

    private Invoice(Builder builder) {
        this.number = builder.number;
        this.reservationId = builder.reservationId;
        this.guest = builder.guest;
        this.roomNumber = builder.roomNumber;
        this.checkInDate = builder.checkInDate;
        this.checkOutDate = builder.checkOutDate;
        this.roomLines = new ArrayList<>(builder.roomLines);
        this.serviceLines = new ArrayList<>(builder.serviceLines);
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
        return List.copyOf(roomLines);
    }

    public List<InvoiceLine> getServiceLines() {
        return List.copyOf(serviceLines);
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
