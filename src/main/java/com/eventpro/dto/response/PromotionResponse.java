package com.eventpro.dto.response;

import com.eventpro.domain.enums.DiscountType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Builder
public class PromotionResponse {
    private final Long id;
    private final String code;
    private final DiscountType discountType;
    private final BigDecimal discountValue;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final int usageLimit;
    private final int usageCount;
}
