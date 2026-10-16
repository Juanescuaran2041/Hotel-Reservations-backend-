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

    /** Makes sure the next number is greater than every number already stored (called at startup). */
    public void resumeAfter(long lastIssuedSequence) {
        sequence.accumulateAndGet(lastIssuedSequence, Math::max);
    }

    public String next() {
        return "INV-%d-%06d".formatted(Year.now().getValue(), sequence.incrementAndGet());
    }
}
