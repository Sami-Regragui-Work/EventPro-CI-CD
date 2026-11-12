package com.eventpro.controller;

import com.eventpro.config.ClockConfig;
import com.eventpro.domain.enums.ReservationStatus;
import com.eventpro.dto.response.reservations.ReservationResponse;
import com.eventpro.entity.reservations.Reservation;
import com.eventpro.exception.badrequest.ValidationException;
import com.eventpro.exception.conflict.CancellationTooLateException;
import com.eventpro.exception.conflict.QuotaExceededException;
import com.eventpro.exception.notfound.ResourceNotFoundException;
import com.eventpro.mapper.ReservationMapper;
import com.eventpro.service.ReservationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReservationController.class)
@Import(ClockConfig.class)
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReservationService service;

    @MockitoBean
    private ReservationMapper mapper;

    private ReservationResponse sampleResponse(ReservationStatus status) {
        return ReservationResponse.builder()
                .id(1L)
                .reference("RES-B3C9D2")
                .status(status)
                .expirationDate(Instant.parse("2026-06-01T09:15:00Z"))
                .totalAmount(new BigDecimal("120.00"))
                .promotionCode(null)
                .reservationLines(null)
                .build();
    }

    @Test
    void createReturns201WithMappedReservation() throws Exception {
        Reservation reservation = Reservation.builder().id(1L).reference("RES-B3C9D2").status(ReservationStatus.PENDING_PAYMENT).totalAmount(new BigDecimal("120.00")).build();
        given(this.service.create(eq(1L), eq(1L), any())).willReturn(reservation);
        given(this.mapper.toResponse(reservation)).willReturn(this.sampleResponse(ReservationStatus.PENDING_PAYMENT));

        this.mockMvc.perform(post("/api/clients/1/events/1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reservationLines\":[{\"eventTicketCategoryId\":7,\"quantity\":2}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reference").value("RES-B3C9D2"))
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"));
    }

    @Test
    void createWithNoLinesReturns400WithFieldErrors() throws Exception {
        this.mockMvc.perform(post("/api/clients/1/events/1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reservationLines\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.path").value("/api/clients/1/events/1/reservations"))
                .andExpect(jsonPath("$.fieldErrors.reservationLines").value("At least one ticket category is required"));
    }

    @Test
    void createWithQuantityBelowOneReturns400WithFieldErrors() throws Exception {
        this.mockMvc.perform(post("/api/clients/1/events/1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reservationLines\":[{\"eventTicketCategoryId\":7,\"quantity\":0}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors['reservationLines[0].quantity']").value("Quantity must be at least 1"));
    }

    @Test
    void createWhenClientExceedsQuotaReturns409Shape() throws Exception {
        willThrow(new QuotaExceededException("Quota of 4 exceeded for client 1")).given(this.service).create(any(), any(), any());

        this.mockMvc.perform(post("/api/clients/1/events/1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reservationLines\":[{\"eventTicketCategoryId\":7,\"quantity\":2}]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Quota of 4 exceeded for client 1"))
                .andExpect(jsonPath("$.path").value("/api/clients/1/events/1/reservations"));
    }

    @Test
    void getByReferenceReturnsReservation() throws Exception {
        Reservation reservation = Reservation.builder().id(1L).reference("RES-B3C9D2").status(ReservationStatus.CONFIRMED).build();
        given(this.service.getByReference(1L, 1L, "RES-B3C9D2")).willReturn(reservation);
        given(this.mapper.toResponse(reservation)).willReturn(this.sampleResponse(ReservationStatus.CONFIRMED));

        this.mockMvc.perform(get("/api/clients/1/events/1/reservations/RES-B3C9D2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void getByReferenceWhenUnknownReturns404Shape() throws Exception {
        given(this.service.getByReference(1L, 1L, "RES-UNKNOWN"))
                .willThrow(new ResourceNotFoundException("Reservation RES-UNKNOWN not found"));

        this.mockMvc.perform(get("/api/clients/1/events/1/reservations/RES-UNKNOWN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Reservation RES-UNKNOWN not found"))
                .andExpect(jsonPath("$.path").value("/api/clients/1/events/1/reservations/RES-UNKNOWN"));
    }

    @Test
    void cancelReturns200WithRefreshedReservation() throws Exception {
        Reservation canceled = Reservation.builder().id(1L).reference("RES-B3C9D2").status(ReservationStatus.CANCELED).build();
        given(this.service.cancelByClient(1L, 1L, "RES-B3C9D2")).willReturn(canceled);
        given(this.mapper.toResponse(canceled)).willReturn(this.sampleResponse(ReservationStatus.CANCELED));

        this.mockMvc.perform(post("/api/clients/1/events/1/reservations/RES-B3C9D2/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));
    }

    @Test
    void cancelWhenTooLateReturns409Shape() throws Exception {
        willThrow(new CancellationTooLateException("Cancellation is only allowed until 48 hours before the event"))
                .given(this.service).cancelByClient(1L, 1L, "RES-B3C9D2");

        this.mockMvc.perform(post("/api/clients/1/events/1/reservations/RES-B3C9D2/cancel"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Cancellation is only allowed until 48 hours before the event"))
                .andExpect(jsonPath("$.path").value("/api/clients/1/events/1/reservations/RES-B3C9D2/cancel"));
    }
}
