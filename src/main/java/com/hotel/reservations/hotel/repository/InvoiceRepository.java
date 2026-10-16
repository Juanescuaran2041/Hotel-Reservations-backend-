package com.hotel.reservations.hotel.repository;

import java.util.Optional;
import java.util.UUID;

import com.hotel.reservations.hotel.model.Invoice;

public interface InvoiceRepository {

    Invoice save(Invoice invoice);

    Optional<Invoice> findByReservationId(UUID reservationId);
}
