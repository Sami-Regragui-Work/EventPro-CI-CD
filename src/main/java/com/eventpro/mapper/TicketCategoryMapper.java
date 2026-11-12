package com.eventpro.mapper;

import com.eventpro.dto.response.tickets.TicketCategoryResponse;
import com.eventpro.entity.tickets.TicketCategory;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TicketCategoryMapper {
    TicketCategoryResponse toResponse(TicketCategory ticketCategory);
}