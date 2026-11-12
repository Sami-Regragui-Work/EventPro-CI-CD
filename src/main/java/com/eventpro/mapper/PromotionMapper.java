package com.eventpro.mapper;

import com.eventpro.dto.request.CreatePromotionRequest;
import com.eventpro.dto.response.PromotionResponse;
import com.eventpro.entity.transactions.Promotion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PromotionMapper {
    PromotionResponse toResponse(Promotion promotion);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "usageCount", ignore = true)
    @Mapping(target = "reservations", ignore = true)
    Promotion toEntity(CreatePromotionRequest dto);
}
