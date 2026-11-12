package com.eventpro.mapper;

import com.eventpro.dto.response.tickets.TicketResponse;
import com.eventpro.entity.tickets.Ticket;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TicketMapper {
    TicketResponse toResponse(Ticket ticket);
}
