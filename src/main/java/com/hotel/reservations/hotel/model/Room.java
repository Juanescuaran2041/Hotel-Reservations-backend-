package com.hotel.reservations.hotel.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A hotel room. Availability is not stored here: it is derived from the reservations that hold the room. */
@Entity
@Table(name = "rooms")
public class Room {

    @Id
    @Column(length = 10)
    private String number;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RoomType type;

    protected Room() {
    }

    public Room(String number, RoomType type) {
        this.number = number;
        this.type = type;
    }

    public String getNumber() {
        return number;
    }

    public RoomType getType() {
        return type;
    }
}
