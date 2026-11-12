package com.eventpro.domain.enums;

import java.util.EnumSet;
import java.util.Set;

public enum EventStatus {
    DRAFT,
    PUBLISHED,
    COMPLETED,
    CANCELED;

    private Set<EventStatus> allowedNext() {
        return switch (this) {
            case DRAFT -> EnumSet.of(PUBLISHED, CANCELED);
            case PUBLISHED -> EnumSet.of(COMPLETED, CANCELED);
            case CANCELED, COMPLETED -> EnumSet.noneOf(EventStatus.class);
        };
    }

    public boolean canTransitionTo(EventStatus desiredStatus) {
        return this.allowedNext().contains(desiredStatus);
    }

    public boolean canBeEdited() {
        return this == DRAFT || this == PUBLISHED;
    }
}
