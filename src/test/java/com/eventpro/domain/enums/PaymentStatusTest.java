package com.eventpro.domain.enums;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentStatusTest {
    static Stream<Arguments> transitionTable() {
        return Stream.of(
                Arguments.of(PaymentStatus.PENDING, PaymentStatus.PENDING, false),
                Arguments.of(PaymentStatus.PENDING, PaymentStatus.SUCCESS, true),
                Arguments.of(PaymentStatus.PENDING, PaymentStatus.FAILED, true),
                Arguments.of(PaymentStatus.SUCCESS, PaymentStatus.PENDING, false),
                Arguments.of(PaymentStatus.SUCCESS, PaymentStatus.SUCCESS, false),
                Arguments.of(PaymentStatus.SUCCESS, PaymentStatus.FAILED, false),
                Arguments.of(PaymentStatus.FAILED, PaymentStatus.PENDING, false),
                Arguments.of(PaymentStatus.FAILED, PaymentStatus.SUCCESS, false),
                Arguments.of(PaymentStatus.FAILED, PaymentStatus.FAILED, false)
        );
    }

    @ParameterizedTest(name = "{0} to {1} is {2}")
    @MethodSource("transitionTable")
    void transitionFollowsTheTable(PaymentStatus currentStatus, PaymentStatus desiredStatus, boolean allowed) {
        assertEquals(allowed, currentStatus.canTransitionTo(desiredStatus));
    }
}
