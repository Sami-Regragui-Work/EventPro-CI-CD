package com.eventpro.dto.request.tickets;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor
public class AttachTicketCategoryRequest {
    @NotBlank(message = "Category name is required")
    @Size(max = 255, message = "Category name must be at most 255 characters")
    private String name;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0", message = "Unit price cannot be negative")
    private BigDecimal unitPrice;

    @NotNull(message = "Total quantity is required")
    @Min(value = 1, message = "Total quantity must be at least 1")
    private Integer totalQuantity;

    @NotNull(message = "Quota per client is required")
    @Min(value = 1, message = "Quota per client must be at least 1")
    private Integer maxQuotaPerClient;
}
