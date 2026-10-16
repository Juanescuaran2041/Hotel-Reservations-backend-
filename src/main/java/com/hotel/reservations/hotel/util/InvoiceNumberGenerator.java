package com.hotel.reservations.hotel.util;

import java.time.Year;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Singleton pattern (initialization-on-demand holder idiom).
 * The hotel has a single, consecutive invoice sequence, so exactly one generator must exist.
 */
public final class InvoiceNumberGenerator {

    private final AtomicLong sequence = new AtomicLong();

    private InvoiceNumberGenerator() {
    }

    private static final class Holder {
        private static final InvoiceNumberGenerator INSTANCE = new InvoiceNumberGenerator();
    }

    public static InvoiceNumberGenerator getInstance() {
        return Holder.INSTANCE;
    }

    public String next() {
        return "INV-%d-%06d".formatted(Year.now().getValue(), sequence.incrementAndGet());
    }
}
