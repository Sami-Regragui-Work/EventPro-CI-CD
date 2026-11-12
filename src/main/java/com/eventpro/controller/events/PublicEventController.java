package com.eventpro.controller.events;

import com.eventpro.dto.response.PageResponse;
import com.eventpro.dto.response.events.EventSummaryResponse;
import com.eventpro.entity.Event;
import com.eventpro.mapper.EventMapper;
import com.eventpro.service.EventService;
import com.eventpro.service.EventTicketCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class PublicEventController {
    private final EventService eventService;
    private final EventTicketCategoryService eventTicketCategoryService;
    private final EventMapper mapper;

    @GetMapping
    public PageResponse<EventSummaryResponse> list(
            @PageableDefault(size = 20, sort = "startDate")
            Pageable pageable
    )
    {
        Page<Event> page = this.eventService.findPublic(pageable);
        Map<Long, Long> availableSeats = this.eventTicketCategoryService.availableSeatsByEventId(page.map(Event::getId).getContent());
        return PageResponse.of(page.map(event ->
                this.mapper.toSummaryResponse(event, availableSeats.getOrDefault(event.getId(), 0L))));
    }

    @GetMapping("/{id}")
    public EventSummaryResponse get(@PathVariable Long id) {
        return this.mapper.toSummaryResponse(this.eventService.findPublicById(id));
    }
}
