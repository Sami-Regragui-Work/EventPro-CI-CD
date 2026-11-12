package com.eventpro.dto.request.tickets;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class TicketCategoryRequest {
    @Size(message = "Category name must be at most 255 characters", max = 255)
    @NotBlank(message = "Category name is required")
    private String name;
}
