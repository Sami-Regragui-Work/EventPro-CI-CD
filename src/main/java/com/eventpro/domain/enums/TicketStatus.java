package com.eventpro.domain.enums;

import java.util.EnumSet;
import java.util.Set;

public enum TicketStatus {
    VALID,
    USED,
    CANCELED;

    private Set<TicketStatus> allowedNextStatuses() {
        return switch (this) {
            case VALID -> EnumSet.of(USED, CANCELED);
            case USED, CANCELED -> EnumSet.noneOf(TicketStatus.class);
        };
    }

    public boolean canTransitionTo(TicketStatus desiredStatus) {
        return this.allowedNextStatuses().contains(desiredStatus);
    }
}
