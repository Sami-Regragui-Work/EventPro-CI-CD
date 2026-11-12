package com.eventpro.controller.events;

import com.eventpro.dto.request.events.CreateEventRequest;
import com.eventpro.dto.request.events.UpdateEventRequest;
import com.eventpro.dto.response.PageResponse;
import com.eventpro.dto.response.events.EventResponse;
import com.eventpro.entity.Event;
import com.eventpro.exception.badrequest.ValidationException;
import com.eventpro.mapper.EventMapper;
import com.eventpro.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/organizers/{organizerId}/events")
@RequiredArgsConstructor
public class OrganizerEventController {
    private final EventService service;
    private final EventMapper mapper;

    @PostMapping
    public ResponseEntity<EventResponse> create(
            @PathVariable Long organizerId,
            @Valid @RequestBody
            CreateEventRequest dto
    ) throws ValidationException
    {
        Event event = this.service.create(organizerId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(this.mapper.toResponse(event));
    }

    @GetMapping
    public PageResponse<EventResponse> list(
            @PathVariable Long organizerId,
            @PageableDefault(size = 20, sort = "startDate")
            Pageable pageable
    ) {
        return PageResponse.of(this.service.findByOrganizer(organizerId, pageable).map(this.mapper::toResponse));
    }

    @PatchMapping("/{eventId}")
    public EventResponse update(
            @PathVariable Long organizerId,
            @PathVariable Long eventId,
            @Valid @RequestBody
            UpdateEventRequest dto
    ) throws ValidationException {
        return this.mapper.toResponse(this.service.update(organizerId, eventId, dto));
    }

    @PostMapping("/{eventId}/publish")
    public EventResponse publish(
            @PathVariable Long organizerId,
            @PathVariable Long eventId
    ) {
        return this.mapper.toResponse(this.service.publish(organizerId, eventId));
    }

    @PostMapping("/{eventId}/cancel")
    public EventResponse cancel(
            @PathVariable Long organizerId,
            @PathVariable Long eventId
    ) {
        return this.mapper.toResponse(this.service.cancel(organizerId, eventId));
    }

    @PostMapping("/{eventId}/end")
    public EventResponse end(
            @PathVariable Long organizerId,
            @PathVariable Long eventId
    ) throws ValidationException {
        return this.mapper.toResponse(this.service.end(organizerId, eventId));
    }
}
