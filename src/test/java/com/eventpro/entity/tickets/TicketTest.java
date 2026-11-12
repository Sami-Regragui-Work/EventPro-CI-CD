package com.eventpro.entity.tickets;

import com.eventpro.domain.enums.TicketStatus;
import com.eventpro.exception.conflict.IllegalTicketStateException;
import com.eventpro.exception.conflict.TicketAlreadyUsedException;
import com.eventpro.exception.conflict.TicketCancelledException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TicketTest {
    private static final Instant NOW = Instant.parse("2026-06-15T10:00:00Z");

    @Test
    void scanMovesAValidTicketToUsedWithAScanDate() {
        Ticket validTicket = TicketTest.ticket(TicketStatus.VALID);

        validTicket.scan(NOW);

        assertEquals(TicketStatus.USED, validTicket.getStatus());
        assertEquals(NOW, validTicket.getScanDate());
    }

    @Test
    void scanRefusesAnAlreadyUsedTicket() {
        Ticket usedTicket = TicketTest.ticket(TicketStatus.USED);

        assertThrows(TicketAlreadyUsedException.class, () -> usedTicket.scan(NOW));
        assertEquals(TicketStatus.USED, usedTicket.getStatus());
        assertNull(usedTicket.getScanDate());
    }

    @Test
    void scanRefusesACanceledTicket() {
        Ticket canceledTicket = TicketTest.ticket(TicketStatus.CANCELED);

        assertThrows(TicketCancelledException.class, () -> canceledTicket.scan(NOW));
        assertEquals(TicketStatus.CANCELED, canceledTicket.getStatus());
        assertNull(canceledTicket.getScanDate());
    }

    @Test
    void transitionRefusesToLeaveAUsedTicket() {
        Ticket usedTicket = TicketTest.ticket(TicketStatus.USED);

        assertThrows(IllegalTicketStateException.class, () -> usedTicket.transitionTo(TicketStatus.CANCELED));
        assertEquals(TicketStatus.USED, usedTicket.getStatus());
    }

    @Test
    void transitionMovesAValidTicketToCanceled() {
        Ticket validTicket = TicketTest.ticket(TicketStatus.VALID);

        validTicket.transitionTo(TicketStatus.CANCELED);

        assertEquals(TicketStatus.CANCELED, validTicket.getStatus());
        assertNull(validTicket.getScanDate());
        assertNotNull(validTicket.getIssueDate());
    }

    private static Ticket ticket(TicketStatus status) {
        return Ticket.builder()
                .code("TKT-TESTCODE01")
                .issueDate(TicketTest.NOW)
                .status(status)
                .build();
    }
}
