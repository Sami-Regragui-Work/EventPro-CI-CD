package com.eventpro.dto.response.reservations;

import com.eventpro.domain.enums.ReservationStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter @Builder
public class ReservationResponse {
    private final Long id;
    private final String reference;
    private final ReservationStatus status;
    private final Instant expirationDate;
    private final String promotionCode;
    private final BigDecimal totalAmount;
    private final List<ReservationLineResponse> reservationLines;
}
