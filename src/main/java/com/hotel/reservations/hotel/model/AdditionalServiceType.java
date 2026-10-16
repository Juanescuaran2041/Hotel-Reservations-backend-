package com.hotel.reservations.hotel.model;

import java.math.BigDecimal;

/** Catalog of extra services. A per-night service is multiplied by the number of nights of the stay. */
public enum AdditionalServiceType {
    SPA("Spa", new BigDecimal("50.00"), false),
    BREAKFAST("Breakfast", new BigDecimal("15.00"), true),
    TRANSPORT("Transport", new BigDecimal("30.00"), false);

    private final String displayName;
    private final BigDecimal unitPrice;
    private final boolean chargedPerNight;

    AdditionalServiceType(String displayName, BigDecimal unitPrice, boolean chargedPerNight) {
        this.displayName = displayName;
        this.unitPrice = unitPrice;
        this.chargedPerNight = chargedPerNight;
    }

    public String getDisplayName() {
        return displayName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public boolean isChargedPerNight() {
        return chargedPerNight;
    }
}
