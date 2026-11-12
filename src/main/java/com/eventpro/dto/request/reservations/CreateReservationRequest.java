package com.eventpro.dto.request.reservations;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter @Setter @NoArgsConstructor
public class CreateReservationRequest {
    private String promotionCode;

    @NotEmpty(message = "At least one ticket category is required")
    @Valid
    private List<ReservationLineRequest> reservationLines;
}
