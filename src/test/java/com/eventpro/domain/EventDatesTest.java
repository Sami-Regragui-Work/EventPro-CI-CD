package com.eventpro.domain;

import com.eventpro.exception.badrequest.ValidationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EventDatesTest {
    private static final Instant START = Instant.parse("2026-07-01T18:00:00Z");
    private static final Instant END = Instant.parse("2026-07-01T21:00:00Z");
    private static final Instant SALES_OPEN = Instant.parse("2026-06-01T09:00:00Z");
    private static final Instant SALES_CLOSE = Instant.parse("2026-07-01T12:00:00Z");

    @Test
    void rejectsEndBeforeStart() {
        assertThrows(ValidationException.class,
                () -> EventDates.of(START, START.minusSeconds(1), SALES_OPEN, SALES_CLOSE));
        assertThrows(ValidationException.class,
                () -> EventDates.of(START, START, SALES_OPEN, SALES_CLOSE));
    }

    @Test
    void rejectsSalesOpeningAfterClosing() {
        assertThrows(ValidationException.class,
                () -> EventDates.of(START, END, SALES_CLOSE, SALES_CLOSE));
        assertThrows(ValidationException.class,
                () -> EventDates.of(START, END, SALES_CLOSE, SALES_OPEN));
    }

    @Test
    void rejectsSalesClosingAfterStart() {
        assertThrows(ValidationException.class,
                () -> EventDates.of(START, END, SALES_OPEN, START.plusSeconds(1)));
    }

    @Test
    void acceptsValidDates() throws ValidationException {
        EventDates dates = EventDates.of(START, END, SALES_OPEN, SALES_CLOSE);

        assertEquals(START, dates.start());
        assertEquals(END, dates.end());
        assertEquals(SALES_OPEN, dates.salesOpen());
        assertEquals(SALES_CLOSE, dates.salesClose());
    }

    @Test
    void overrideValidatesMergedResult() throws ValidationException {
        EventDates dates = EventDates.of(START, END, SALES_OPEN, SALES_CLOSE);

        EventDates merged = dates.override(null, null, null, null);
        assertEquals(dates, merged);

        assertThrows(ValidationException.class,
                () -> dates.override(START, START.minusSeconds(1), null, null));
        assertThrows(ValidationException.class,
                () -> dates.override(null, null, null, END));
    }
}
