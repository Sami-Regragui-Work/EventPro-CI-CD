package com.eventpro.dto.response.tickets;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter @Builder
public class EventTicketCategoryResponse {
    private final Long id;
    private final Long ticketCategoryId;
    private final String ticketCategoryName;
    private final BigDecimal unitPrice;
    private final int totalQuantity;
    private final int maxQuotaPerClient;

    private final long reservedQuantity;
    private final long soldQuantity;
    private final long availableQuantity;
}
