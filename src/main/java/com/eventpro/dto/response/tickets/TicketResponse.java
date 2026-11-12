package com.eventpro.dto.response.tickets;

import com.eventpro.domain.enums.TicketStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter @Builder
public class TicketResponse {
    private final Long id;
    private final String code;
    private final TicketStatus status;
    private final Instant scanDate;
}
