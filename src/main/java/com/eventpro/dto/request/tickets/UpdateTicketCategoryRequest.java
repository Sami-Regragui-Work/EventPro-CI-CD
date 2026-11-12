package com.eventpro.dto.request.tickets;


import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor
public class UpdateTicketCategoryRequest {

    @DecimalMin(value = "0", message = "Unit price cannot be negative")
    private BigDecimal unitPrice;

    @Min(value = 1, message = "Total quantity must be at least 1")
    private Integer totalQuantity;

    @Min(value = 1, message = "Quota per client must be at least 1")
    private Integer maxQuotaPerClient;
}
