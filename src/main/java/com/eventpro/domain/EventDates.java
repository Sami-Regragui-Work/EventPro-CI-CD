package com.eventpro.domain;

import com.eventpro.exception.badrequest.ValidationException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Objects;

@NullMarked
public record EventDates(Instant start, @Nullable Instant end, Instant salesOpen, Instant salesClose) {
    public static EventDates of(Instant start, @Nullable Instant end, Instant salesOpen, Instant salesClose) throws ValidationException {
        if (end != null && !start.isBefore(end)) {
            throw new ValidationException("Start date must be before end date");
        }
        if (!salesOpen.isBefore(salesClose)) {
            throw new ValidationException("Sales opening must be before sales closing");
        }
        if (salesClose.isAfter(start)) {
            throw new ValidationException("Sales must close at or before the start date");
        }
        return new EventDates(start, end, salesOpen, salesClose);
    }

    public EventDates override(@Nullable Instant start, @Nullable Instant end, @Nullable Instant salesOpen, @Nullable Instant salesClose) throws ValidationException {
        return EventDates.of(
                Objects.requireNonNullElse(start, this.start),
                end != null ? end : this.end,
                Objects.requireNonNullElse(salesOpen, this.salesOpen),
                Objects.requireNonNullElse(salesClose, this.salesClose)
        );
    }
}
