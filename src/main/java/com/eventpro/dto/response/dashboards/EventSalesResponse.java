package com.eventpro.dto.response.dashboards;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter @Builder
public class EventSalesResponse {
    private final Long eventId;
    private final String title;
    private final long ticketsSold;
    private final BigDecimal revenue;
}
