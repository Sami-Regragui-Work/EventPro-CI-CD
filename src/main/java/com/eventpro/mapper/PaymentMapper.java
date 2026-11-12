package com.eventpro.mapper;

import com.eventpro.dto.response.payments.PaymentResponse;
import com.eventpro.entity.transactions.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PaymentMapper {
    PaymentResponse toResponse(Payment payment);
}
