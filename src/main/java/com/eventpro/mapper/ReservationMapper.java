package com.eventpro.mapper;

import com.eventpro.dto.response.reservations.ReservationLineResponse;
import com.eventpro.dto.response.reservations.ReservationResponse;
import com.eventpro.entity.reservations.Reservation;
import com.eventpro.entity.reservations.ReservationLine;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ReservationMapper {
    @Mapping(target = "promotionCode", source = "promotion.code")
    ReservationResponse toResponse(Reservation reservation);

    @Mapping(target = "eventTicketCategoryId", source = "eventTicketCategory.id")
    ReservationLineResponse toLineResponse(ReservationLine reservationLine);
}
