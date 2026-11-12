package com.eventpro.controller.transactions;

import com.eventpro.dto.request.payments.ConfirmPaymentRequest;
import com.eventpro.dto.response.payments.PaymentResponse;
import com.eventpro.entity.transactions.Payment;
import com.eventpro.exception.notfound.ResourceNotFoundException;
import com.eventpro.mapper.PaymentMapper;
import com.eventpro.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clients/{clientId}/events/{eventId}/reservations/{reference}/payment")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService service;
    private final PaymentMapper mapper;

    @PostMapping
    public ResponseEntity<PaymentResponse> initiate(
            @PathVariable Long clientId,
            @PathVariable Long eventId,
            @PathVariable String reference
    ) {
        HttpStatus status;
        try {
            this.service.getPayment(clientId, eventId, reference);
            status = HttpStatus.OK;
        } catch (ResourceNotFoundException absent) {
            status = HttpStatus.CREATED;
        }

        Payment payment = this.service.initiate(clientId, eventId, reference);
        return ResponseEntity.status(status).body(this.mapper.toResponse(payment));
    }

    @PostMapping("/confirm")
    public ResponseEntity<PaymentResponse> confirm(
            @PathVariable Long clientId,
            @PathVariable Long eventId,
            @PathVariable String reference,
            @Valid @RequestBody ConfirmPaymentRequest dto
    ) {
        Payment payment = this.service.confirm(clientId, eventId, reference, dto.getIdempotencyKey());
        return ResponseEntity.ok(this.mapper.toResponse(payment));
    }

    @GetMapping
    public ResponseEntity<PaymentResponse> get(
            @PathVariable Long clientId,
            @PathVariable Long eventId,
            @PathVariable String reference
    ) {
        return ResponseEntity.ok(this.mapper.toResponse(this.service.getPayment(clientId, eventId, reference)));
    }
}
