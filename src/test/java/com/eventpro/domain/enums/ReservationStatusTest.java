package com.eventpro.domain.enums;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReservationStatusTest {
    static Stream<Arguments> transitionTable() {
        return Stream.of(
                Arguments.of(ReservationStatus.PENDING_PAYMENT, ReservationStatus.PENDING_PAYMENT, false),
                Arguments.of(ReservationStatus.PENDING_PAYMENT, ReservationStatus.CONFIRMED, true),
                Arguments.of(ReservationStatus.PENDING_PAYMENT, ReservationStatus.EXPIRED, true),
                Arguments.of(ReservationStatus.PENDING_PAYMENT, ReservationStatus.CANCELED, true),
                Arguments.of(ReservationStatus.PENDING_PAYMENT, ReservationStatus.REFUNDED, false),
                Arguments.of(ReservationStatus.CONFIRMED, ReservationStatus.PENDING_PAYMENT, false),
                Arguments.of(ReservationStatus.CONFIRMED, ReservationStatus.CONFIRMED, false),
                Arguments.of(ReservationStatus.CONFIRMED, ReservationStatus.EXPIRED, false),
                Arguments.of(ReservationStatus.CONFIRMED, ReservationStatus.CANCELED, true),
                Arguments.of(ReservationStatus.CONFIRMED, ReservationStatus.REFUNDED, true),
                Arguments.of(ReservationStatus.EXPIRED, ReservationStatus.PENDING_PAYMENT, false),
                Arguments.of(ReservationStatus.EXPIRED, ReservationStatus.CONFIRMED, false),
                Arguments.of(ReservationStatus.EXPIRED, ReservationStatus.EXPIRED, false),
                Arguments.of(ReservationStatus.EXPIRED, ReservationStatus.CANCELED, false),
                Arguments.of(ReservationStatus.EXPIRED, ReservationStatus.REFUNDED, false),
                Arguments.of(ReservationStatus.CANCELED, ReservationStatus.PENDING_PAYMENT, false),
                Arguments.of(ReservationStatus.CANCELED, ReservationStatus.CONFIRMED, false),
                Arguments.of(ReservationStatus.CANCELED, ReservationStatus.EXPIRED, false),
                Arguments.of(ReservationStatus.CANCELED, ReservationStatus.CANCELED, false),
                Arguments.of(ReservationStatus.CANCELED, ReservationStatus.REFUNDED, false),
                Arguments.of(ReservationStatus.REFUNDED, ReservationStatus.PENDING_PAYMENT, false),
                Arguments.of(ReservationStatus.REFUNDED, ReservationStatus.CONFIRMED, false),
                Arguments.of(ReservationStatus.REFUNDED, ReservationStatus.EXPIRED, false),
                Arguments.of(ReservationStatus.REFUNDED, ReservationStatus.CANCELED, false),
                Arguments.of(ReservationStatus.REFUNDED, ReservationStatus.REFUNDED, false)
        );
    }

    @ParameterizedTest(name = "{0} to {1} is {2}")
    @MethodSource("transitionTable")
    void transitionFollowsTheTable(ReservationStatus currentStatus, ReservationStatus desiredStatus, boolean allowed) {
        assertEquals(allowed, currentStatus.canTransitionTo(desiredStatus));
    }
}
