package com.eventpro.dto.response.payments;

import com.eventpro.domain.enums.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter @Builder
public class PaymentResponse {
    private final Long id;
    private final String reference;
    private final String idempotencyKey;
    private final BigDecimal amount;
    private final PaymentStatus status;
    private final Instant paymentDate;
}
