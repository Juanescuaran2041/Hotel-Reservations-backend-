package com.hotel.reservations.hotel.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Aggregate that tracks the whole life cycle of a stay. */
public class Reservation {

    private final UUID id;
    private final String ownerEmail;
    private final Guest guest;
    private final Room room;
    private final LocalDate checkInDate;
    private final LocalDate checkOutDate;
    private final RateQuote rateQuote;
    private final List<ServiceCharge> services = new ArrayList<>();
    private ReservationStatus status = ReservationStatus.CONFIRMED;
    private DigitalKey digitalKey;

    public Reservation(UUID id, String ownerEmail, Guest guest, Room room,
                       LocalDate checkInDate, LocalDate checkOutDate, RateQuote rateQuote) {
        this.id = id;
        this.ownerEmail = ownerEmail;
        this.guest = guest;
        this.room = room;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.rateQuote = rateQuote;
    }

    public long getNights() {
        return ChronoUnit.DAYS.between(checkInDate, checkOutDate);
    }

    public synchronized void addService(ServiceCharge charge) {
        services.add(charge);
    }

    public synchronized List<ServiceCharge> getServices() {
        return List.copyOf(services);
    }

    public synchronized BigDecimal getServicesTotal() {
        return services.stream().map(ServiceCharge::total).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public synchronized BigDecimal getRunningTotal() {
        return getRoomCost().add(getServicesTotal());
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
        return rateQuote;
    }

    public BigDecimal getRoomCost() {
        return rateQuote.total();
    }

    public synchronized ReservationStatus getStatus() {
        return status;
    }

    public synchronized void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public synchronized DigitalKey getDigitalKey() {
        return digitalKey;
    }

    public synchronized void setDigitalKey(DigitalKey digitalKey) {
        this.digitalKey = digitalKey;
    }
}
