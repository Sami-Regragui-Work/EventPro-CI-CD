package com.eventpro.controller.transactions;

import com.eventpro.config.ClockConfig;
import com.eventpro.domain.enums.PaymentStatus;
import com.eventpro.dto.response.payments.PaymentResponse;
import com.eventpro.entity.transactions.Payment;
import com.eventpro.exception.notfound.ResourceNotFoundException;
import com.eventpro.mapper.PaymentMapper;
import com.eventpro.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@Import(ClockConfig.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService service;

    @MockitoBean
    private PaymentMapper mapper;

    private Payment payment() {
        return Payment.builder().id(1L).reference("PAY-7K2M4X").idempotencyKey("IDM-9QW3ZP").amount(new BigDecimal("120.00")).status(PaymentStatus.PENDING).build();
    }

    private PaymentResponse response(PaymentStatus status) {
        return PaymentResponse.builder()
                .id(1L)
                .reference("PAY-7K2M4X")
                .idempotencyKey("IDM-9QW3ZP")
                .amount(new BigDecimal("120.00"))
                .status(status)
                .build();
    }

    @Test
    void initiateReturns201WhenReservationHasNoPaymentYet() throws Exception {
        Payment pending = this.payment();
        willThrow(new ResourceNotFoundException("Payment for reservation RES-B3C9D2 not found"))
                .given(this.service).getPayment(anyLong(), anyLong(), anyString());
        given(this.service.initiate(1L, 1L, "RES-B3C9D2")).willReturn(pending);
        given(this.mapper.toResponse(pending)).willReturn(this.response(PaymentStatus.PENDING));

        this.mockMvc.perform(post("/api/clients/1/events/1/reservations/RES-B3C9D2/payment"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reference").value("PAY-7K2M4X"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void initiateReturns200WhenPaymentAlreadyInitiated() throws Exception {
        Payment pending = this.payment();
        given(this.service.getPayment(1L, 1L, "RES-B3C9D2")).willReturn(pending);
        given(this.service.initiate(1L, 1L, "RES-B3C9D2")).willReturn(pending);
        given(this.mapper.toResponse(pending)).willReturn(this.response(PaymentStatus.PENDING));

        this.mockMvc.perform(post("/api/clients/1/events/1/reservations/RES-B3C9D2/payment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reference").value("PAY-7K2M4X"));
    }

    @Test
    void initiateWhenReservationUnknownReturns404Shape() throws Exception {
        willThrow(new ResourceNotFoundException("Reservation RES-UNKNOWN not found"))
                .given(this.service).getPayment(anyLong(), anyLong(), anyString());
        willThrow(new ResourceNotFoundException("Reservation RES-UNKNOWN not found"))
                .given(this.service).initiate(anyLong(), anyLong(), anyString());

        this.mockMvc.perform(post("/api/clients/1/events/1/reservations/RES-UNKNOWN/payment"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Reservation RES-UNKNOWN not found"))
                .andExpect(jsonPath("$.path").value("/api/clients/1/events/1/reservations/RES-UNKNOWN/payment"));
    }

    @Test
    void confirmReturns200WithConfirmedPayment() throws Exception {
        Payment confirmed = Payment.builder().id(1L).reference("PAY-7K2M4X").idempotencyKey("IDM-9QW3ZP").amount(new BigDecimal("120.00")).status(PaymentStatus.SUCCESS).build();
        given(this.service.confirm(1L, 1L, "RES-B3C9D2", "IDM-9QW3ZP")).willReturn(confirmed);
        given(this.mapper.toResponse(eq(confirmed))).willReturn(this.response(PaymentStatus.SUCCESS));

        this.mockMvc.perform(post("/api/clients/1/events/1/reservations/RES-B3C9D2/payment/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotencyKey\":\"IDM-9QW3ZP\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.idempotencyKey").value("IDM-9QW3ZP"));
    }

    @Test
    void confirmWithBlankIdempotencyKeyReturns400WithFieldErrors() throws Exception {
        this.mockMvc.perform(post("/api/clients/1/events/1/reservations/RES-B3C9D2/payment/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotencyKey\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.path").value("/api/clients/1/events/1/reservations/RES-B3C9D2/payment/confirm"))
                .andExpect(jsonPath("$.fieldErrors.idempotencyKey").value("Idempotency key is required"));
    }

    @Test
    void getReturns200WithExistingPayment() throws Exception {
        Payment pending = this.payment();
        given(this.service.getPayment(1L, 1L, "RES-B3C9D2")).willReturn(pending);
        given(this.mapper.toResponse(pending)).willReturn(this.response(PaymentStatus.PENDING));

        this.mockMvc.perform(get("/api/clients/1/events/1/reservations/RES-B3C9D2/payment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reference").value("PAY-7K2M4X"));
    }

    @Test
    void getWhenPaymentUnknownReturns404Shape() throws Exception {
        given(this.service.getPayment(1L, 1L, "RES-B3C9D2"))
                .willThrow(new ResourceNotFoundException("Payment for reservation RES-B3C9D2 not found"));

        this.mockMvc.perform(get("/api/clients/1/events/1/reservations/RES-B3C9D2/payment"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.path").value("/api/clients/1/events/1/reservations/RES-B3C9D2/payment"));
    }
}
