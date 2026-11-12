package com.eventpro.dto.response.events;

import com.eventpro.domain.enums.EventStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter @Builder
public class EventResponse {
    private final Long id;
    private final String title;
    private final String description;
    private final String location;
    private final Instant startDate;
    private final Instant endDate;
    private final Instant salesOpeningDate;
    private final Instant salesClosingDate;
    private final EventStatus status;
    private final int capacity;
    private final Long organizerId;
}
