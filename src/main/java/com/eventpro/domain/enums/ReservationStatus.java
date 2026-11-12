package com.eventpro.domain.enums;

import java.util.EnumSet;
import java.util.Set;

public enum ReservationStatus {
    PENDING_PAYMENT,
    CONFIRMED,
    EXPIRED,
    CANCELED,
    REFUNDED;

    private Set<ReservationStatus> allowedNextStatuses() {
        return switch (this) {
            case PENDING_PAYMENT -> EnumSet.of(CONFIRMED, EXPIRED, CANCELED);
            case CONFIRMED -> EnumSet.of(CANCELED, REFUNDED);
            case EXPIRED, CANCELED, REFUNDED -> EnumSet.noneOf(ReservationStatus.class);
        };
    }

    public boolean canTransitionTo(ReservationStatus desiredStatus) {
        return this.allowedNextStatuses().contains(desiredStatus);
    }
}
