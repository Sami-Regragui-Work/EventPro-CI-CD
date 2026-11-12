package com.eventpro.dto.request.events;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter @Setter @NoArgsConstructor
public class CreateEventRequest {
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must be at most 255 characters")
    private String title;

    @Size(max = 255, message = "Description must be at most 255 characters")
    private String description;

    @NotBlank(message = "Location is required")
    @Size(max = 255, message = "Location must be at most 255 characters")
    private String location;

    @NotNull(message = "Start date is required")
    private Instant startDate;

    private Instant endDate;

    @NotNull(message = "Sales opening date is required")
    private Instant salesOpeningDate;

    @NotNull(message = "Sales closing date is required")
    private Instant salesClosingDate;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;
}
