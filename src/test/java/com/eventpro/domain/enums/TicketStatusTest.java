package com.eventpro.domain.enums;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TicketStatusTest {
    static Stream<Arguments> transitionTable() {
        return Stream.of(
                Arguments.of(TicketStatus.VALID, TicketStatus.VALID, false),
                Arguments.of(TicketStatus.VALID, TicketStatus.USED, true),
                Arguments.of(TicketStatus.VALID, TicketStatus.CANCELED, true),
                Arguments.of(TicketStatus.USED, TicketStatus.VALID, false),
                Arguments.of(TicketStatus.USED, TicketStatus.USED, false),
                Arguments.of(TicketStatus.USED, TicketStatus.CANCELED, false),
                Arguments.of(TicketStatus.CANCELED, TicketStatus.VALID, false),
                Arguments.of(TicketStatus.CANCELED, TicketStatus.USED, false),
                Arguments.of(TicketStatus.CANCELED, TicketStatus.CANCELED, false)
        );
    }

    @ParameterizedTest(name = "{0} to {1} is {2}")
    @MethodSource("transitionTable")
    void transitionFollowsTheTable(TicketStatus currentStatus, TicketStatus desiredStatus, boolean allowed) {
        assertEquals(allowed, currentStatus.canTransitionTo(desiredStatus));
    }
}
