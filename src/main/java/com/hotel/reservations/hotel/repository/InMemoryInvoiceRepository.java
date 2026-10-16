package com.hotel.reservations.hotel.repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.hotel.reservations.hotel.model.Invoice;

@Repository
public class InMemoryInvoiceRepository implements InvoiceRepository {

    private final Map<UUID, Invoice> store = new ConcurrentHashMap<>();

    @Override
    public Invoice save(Invoice invoice) {
        store.put(invoice.getReservationId(), invoice);
        return invoice;
    }

    @Override
    public Optional<Invoice> findByReservationId(UUID reservationId) {
        return Optional.ofNullable(store.get(reservationId));
    }
}
