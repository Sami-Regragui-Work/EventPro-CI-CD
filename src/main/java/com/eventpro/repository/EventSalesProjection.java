package com.eventpro.repository;

import java.math.BigDecimal;

public interface EventSalesProjection {
    Long getEventId();
    String getTitle();
    Long getTicketsSold();
    BigDecimal getRevenue();
}
