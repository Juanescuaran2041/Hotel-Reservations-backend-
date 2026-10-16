package com.hotel.reservations.hotel.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Aggregate that tracks the whole life cycle of a stay. The version column gives optimistic locking, so two
 * concurrent updates of the same reservation cannot silently overwrite each other.
 */
@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    private UUID id;

    @Version
    private Long version;

    @Column(name = "owner_email", nullable = false)
    private String ownerEmail;

    @Embedded
    private Guest guest;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "room_number")
    private Room room;

    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "check_out_date", nullable = false)
    private LocalDate checkOutDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status = ReservationStatus.CONFIRMED;

    @Column(name = "room_cost", nullable = false)
    private BigDecimal roomCost;

    @ElementCollection
    @CollectionTable(name = "reservation_rate_lines", joinColumns = @JoinColumn(name = "reservation_id"))
    @OrderColumn(name = "line_order")
    private List<RateLine> rateLines = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "reservation_services", joinColumns = @JoinColumn(name = "reservation_id"))
    @OrderColumn(name = "line_order")
    private List<ServiceCharge> services = new ArrayList<>();

    @Column(name = "digital_key_code")
    private String digitalKeyCode;

    @Column(name = "digital_key_issued_at")
    private Instant digitalKeyIssuedAt;

    protected Reservation() {
    }

    public Reservation(UUID id, String ownerEmail, Guest guest, Room room,
                       LocalDate checkInDate, LocalDate checkOutDate, RateQuote rateQuote) {
        this.id = id;
        this.ownerEmail = ownerEmail;
        this.guest = guest;
        this.room = room;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.roomCost = rateQuote.total();
        this.rateLines.addAll(rateQuote.lines());
    }

    public long getNights() {
        return ChronoUnit.DAYS.between(checkInDate, checkOutDate);
    }

    public void addService(ServiceCharge charge) {
        services.add(charge);
    }

    public List<ServiceCharge> getServices() {
        return List.copyOf(services);
    }

    public BigDecimal getServicesTotal() {
        return services.stream().map(ServiceCharge::total).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getRunningTotal() {
        return roomCost.add(getServicesTotal());
    }

    public UUID getId() {
        return id;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public Guest getGuest() {
        return guest;
    }

    public Room getRoom() {
        return room;
    }

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }

    public RateQuote getRateQuote() {
        return new RateQuote(room.getType(), getNights(), List.copyOf(rateLines), roomCost);
    }

    public BigDecimal getRoomCost() {
        return roomCost;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public DigitalKey getDigitalKey() {
        if (digitalKeyCode == null) {
            return null;
        }
        return new DigitalKey(digitalKeyCode, room.getNumber(), digitalKeyIssuedAt);
    }

    public void setDigitalKey(DigitalKey digitalKey) {
        this.digitalKeyCode = digitalKey == null ? null : digitalKey.code();
        this.digitalKeyIssuedAt = digitalKey == null ? null : digitalKey.issuedAt();
    }
}
