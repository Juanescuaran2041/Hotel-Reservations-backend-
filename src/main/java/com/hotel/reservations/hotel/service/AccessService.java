package com.hotel.reservations.hotel.service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.hotel.reservations.hotel.model.DigitalKey;
import com.hotel.reservations.hotel.model.Reservation;

/** Subsystem: simulated digital room keys. */
@Service
public class AccessService {

    private final Map<String, DigitalKey> activeKeys = new ConcurrentHashMap<>();

    public DigitalKey issueKey(Reservation reservation) {
        DigitalKey key = new DigitalKey(UUID.randomUUID().toString(), reservation.getRoom().getNumber(), Instant.now());
        activeKeys.put(key.code(), key);
        return key;
    }

    public void revokeKey(DigitalKey key) {
        if (key != null) {
            activeKeys.remove(key.code());
        }
    }

    public boolean isValid(String code) {
        return activeKeys.containsKey(code);
    }
}
