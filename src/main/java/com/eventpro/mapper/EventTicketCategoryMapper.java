package com.eventpro.mapper;

import com.eventpro.dto.request.tickets.AttachTicketCategoryRequest;
import com.eventpro.dto.request.tickets.UpdateTicketCategoryRequest;
import com.eventpro.dto.response.tickets.EventTicketCategoryResponse;
import com.eventpro.entity.tickets.EventTicketCategory;
import org.mapstruct.*;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface EventTicketCategoryMapper {
    @Mapping(target = "ticketCategoryId", source = "ticketCategory.id")
    @Mapping(target = "ticketCategoryName", source = "ticketCategory.name")
    EventTicketCategoryResponse toResponse(EventTicketCategory eventTicketCategory);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "ticketCategory", ignore = true)
    @Mapping(target = "reservationLines", ignore = true)
    EventTicketCategory toEntity(AttachTicketCategoryRequest dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "ticketCategory", ignore = true)
    @Mapping(target = "reservationLines", ignore = true)
    void updateEntity(UpdateTicketCategoryRequest dto, @MappingTarget EventTicketCategory eventTicketCategory);

    @Mapping(target = "id", source = "eventTicketCategory.id")
    @Mapping(target = "ticketCategoryId", source = "eventTicketCategory.ticketCategory.id")
    @Mapping(target = "ticketCategoryName", source = "eventTicketCategory.ticketCategory.name")
    @Mapping(target = "unitPrice", source = "eventTicketCategory.unitPrice")
    @Mapping(target = "totalQuantity", source = "eventTicketCategory.totalQuantity")
    @Mapping(target = "maxQuotaPerClient", source = "eventTicketCategory.maxQuotaPerClient")
    @Mapping(target = "reservedQuantity", source = "reservedQuantity")
    @Mapping(target = "soldQuantity", source = "soldQuantity")
    @Mapping(target = "availableQuantity", source = "availableQuantity")
    EventTicketCategoryResponse toStockResponse(
            EventTicketCategory eventTicketCategory,
            long reservedQuantity,
            long soldQuantity,
            long availableQuantity
    );
}
