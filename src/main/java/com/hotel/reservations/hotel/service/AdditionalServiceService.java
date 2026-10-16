package com.hotel.reservations.hotel.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.hotel.reservations.common.exception.BusinessRuleException;
import com.hotel.reservations.hotel.model.AdditionalServiceType;
import com.hotel.reservations.hotel.model.Reservation;
import com.hotel.reservations.hotel.model.ReservationStatus;
import com.hotel.reservations.hotel.model.ServiceCharge;

/** Subsystem: extra services (spa, breakfast, transport) charged on top of the room. */
@Service
public class AdditionalServiceService {

    public ServiceCharge addService(Reservation reservation, AdditionalServiceType type) {
        if (reservation.getStatus() == ReservationStatus.CHECKED_OUT) {
            throw new BusinessRuleException("Services cannot be added to a reservation that is already checked out");
        }
        int quantity = type.isChargedPerNight() ? Math.toIntExact(reservation.getNights()) : 1;
        BigDecimal total = type.getUnitPrice().multiply(BigDecimal.valueOf(quantity));
        ServiceCharge charge = new ServiceCharge(type, quantity, type.getUnitPrice(), total);
        reservation.addService(charge);
        return charge;
    }
}
