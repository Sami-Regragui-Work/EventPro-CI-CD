package com.eventpro.dto.request.events;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter @Setter @NoArgsConstructor
public class UpdateEventRequest {
    // Editable in any status
    @Size(max = 255, message = "Description must be at most 255 characters")
    private String description;

    @Pattern(regexp = ".*\\S.*", message = "Location cannot be blank")
    @Size(max = 255, message = "Location must be at most 255 characters")
    private String location;

    // Locked after publication
    @Pattern(regexp = ".*\\S.*", message = "Title cannot be blank")
    @Size(max = 255, message = "Title must be at most 255 characters")
    private String title;

    private Instant startDate;

    private Instant endDate;

    private Instant salesOpeningDate;

    private Instant salesClosingDate;

    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;
}
