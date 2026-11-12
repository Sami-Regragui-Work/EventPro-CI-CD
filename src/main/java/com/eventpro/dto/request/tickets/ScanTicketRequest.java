package com.eventpro.dto.request.tickets;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class ScanTicketRequest {
    @NotBlank(message = "Ticket code is required")
    private String code;
}
