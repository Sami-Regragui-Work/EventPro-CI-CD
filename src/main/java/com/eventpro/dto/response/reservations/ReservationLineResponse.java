package com.eventpro.dto.response.reservations;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter @Builder
public class ReservationLineResponse {
    private final Long eventTicketCategoryId;
    private final int quantity;
    private final BigDecimal fixedUnitPrice;
    private final BigDecimal subtotal;
}