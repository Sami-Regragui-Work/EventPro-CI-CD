package com.eventpro.mapper;

import com.eventpro.dto.request.events.CreateEventRequest;
import com.eventpro.dto.request.events.UpdateEventRequest;
import com.eventpro.dto.response.events.EventResponse;
import com.eventpro.dto.response.events.EventSummaryResponse;
import com.eventpro.entity.Event;
import org.mapstruct.*;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface EventMapper {
    @Mapping(target = "organizerId", source = "organizer.id")
    EventResponse toResponse(Event event);

    @Mapping(target = "organizerName", source = "organizer.name")
    EventSummaryResponse toSummaryResponse(Event event);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "organizer", ignore = true)
    @Mapping(target = "eventTicketCategories", ignore = true)
    @Mapping(target = "promotions", ignore = true)
    Event toEntity(CreateEventRequest dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "organizer", ignore = true)
    @Mapping(target = "eventTicketCategories", ignore = true)
    @Mapping(target = "promotions", ignore = true)
    void updateEntity(UpdateEventRequest dto, @MappingTarget Event event);

    @Mapping(target = "organizerName", source = "event.organizer.name")
    @Mapping(target = "availableSeats", source = "availableSeats")
    EventSummaryResponse toSummaryResponse(Event event, long availableSeats);
}
