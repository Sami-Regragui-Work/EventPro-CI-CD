package com.eventpro.dto.response.events;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter @Builder
public class EventSummaryResponse {
    private final Long id;
    private final String title;
    private final String location;
    private final Instant startDate;
    private final Instant salesClosingDate;
    private final String organizerName;

    private final long availableSeats;
}
