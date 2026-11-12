package com.eventpro.domain.enums;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventStatusTest {
    static Stream<Arguments> transitionTable() {
        return Stream.of(
                Arguments.of(EventStatus.DRAFT, EventStatus.DRAFT, false),
                Arguments.of(EventStatus.DRAFT, EventStatus.PUBLISHED, true),
                Arguments.of(EventStatus.DRAFT, EventStatus.COMPLETED, false),
                Arguments.of(EventStatus.DRAFT, EventStatus.CANCELED, true),
                Arguments.of(EventStatus.PUBLISHED, EventStatus.DRAFT, false),
                Arguments.of(EventStatus.PUBLISHED, EventStatus.PUBLISHED, false),
                Arguments.of(EventStatus.PUBLISHED, EventStatus.COMPLETED, true),
                Arguments.of(EventStatus.PUBLISHED, EventStatus.CANCELED, true),
                Arguments.of(EventStatus.COMPLETED, EventStatus.DRAFT, false),
                Arguments.of(EventStatus.COMPLETED, EventStatus.PUBLISHED, false),
                Arguments.of(EventStatus.COMPLETED, EventStatus.COMPLETED, false),
                Arguments.of(EventStatus.COMPLETED, EventStatus.CANCELED, false),
                Arguments.of(EventStatus.CANCELED, EventStatus.DRAFT, false),
                Arguments.of(EventStatus.CANCELED, EventStatus.PUBLISHED, false),
                Arguments.of(EventStatus.CANCELED, EventStatus.COMPLETED, false),
                Arguments.of(EventStatus.CANCELED, EventStatus.CANCELED, false)
        );
    }

    @ParameterizedTest(name = "{0} to {1} is {2}")
    @MethodSource("transitionTable")
    void transitionFollowsTheTable(EventStatus currentStatus, EventStatus desiredStatus, boolean allowed) {
        assertEquals(allowed, currentStatus.canTransitionTo(desiredStatus));
    }
}
