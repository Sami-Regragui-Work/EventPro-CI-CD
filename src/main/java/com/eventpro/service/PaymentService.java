package com.eventpro.service;

import com.eventpro.domain.CodeGenerator;
import com.eventpro.domain.enums.PaymentStatus;
import com.eventpro.domain.enums.ReservationStatus;
import com.eventpro.entity.reservations.Reservation;
import com.eventpro.entity.transactions.Payment;
import com.eventpro.exception.conflict.IdempotencyConflictException;
import com.eventpro.exception.conflict.IllegalReservationStateException;
import com.eventpro.exception.conflict.ReservationExpiredException;
import com.eventpro.exception.internalservererror.SystemException;
import com.eventpro.exception.notfound.ResourceNotFoundException;
import com.eventpro.repository.PaymentRepository;
import com.eventpro.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class PaymentService {
    private static final String REFERENCE_PREFIX = "PAY-";
    private static final String IDEMPOTENCY_PREFIX = "IDM-";
    private static final int CODE_LENGTH = 10;
    private static final int MAX_ATTEMPTS = 5;

    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;
    private final TicketService ticketService;
    private final Clock clock;

    public Payment initiate(Long clientId, Long eventId, String reference) {
        Reservation reservation = this.requireLockedReservation(clientId, eventId, reference);

        if (reservation.getStatus() != ReservationStatus.PENDING_PAYMENT)
            throw new IllegalReservationStateException();

        if (reservation.isExpiredAt(this.clock.instant()))
            throw new ReservationExpiredException();

        Payment existingPayment = this.paymentRepository.findWithLockByReservationId(reservation.getId()).orElse(null);
        if (existingPayment != null)
            return existingPayment;

        return this.createPendingPayment(reservation);
    }

    public Payment confirm(Long clientId, Long eventId, String reference, String idempotencyKey) {
        Reservation reservation = this.requireLockedReservation(clientId, eventId, reference);

        Payment payment = this.paymentRepository.findWithLockByReservationId(reservation.getId())
                .orElseThrow(() -> new IdempotencyConflictException("No payment exists for this reservation"));

        if (!payment.getIdempotencyKey().equals(idempotencyKey))
            throw new IdempotencyConflictException("Idempotency key does not match the payment of this reservation");

        if (payment.getStatus() == PaymentStatus.SUCCESS)
            return payment;
        if (payment.getStatus() == PaymentStatus.FAILED)
            throw new ReservationExpiredException();

        if (reservation.isExpiredAt(this.clock.instant()))
            throw new ReservationExpiredException();

        Instant now = this.clock.instant();
        payment.transitionTo(PaymentStatus.SUCCESS);
        reservation.transitionTo(ReservationStatus.CONFIRMED);
        this.ticketService.issueTickets(reservation, now);
        return payment;
    }

    @Transactional(readOnly = true)
    public Payment getPayment(Long clientId, Long eventId, String reference) {
        Reservation reservation = this.findOwnedReservation(clientId, eventId, reference);
        return this.paymentRepository.findByReservationId(reservation.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment for reservation " + reference + " not found"));
    }

    private Reservation requireLockedReservation(Long clientId, Long eventId, String reference) {
        Reservation reservation = this.reservationRepository.findWithLockByReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation " + reference + " not found"));
        PaymentService.checkOwnership(reservation, clientId, eventId, reference);
        return reservation;
    }

    private Reservation findOwnedReservation(Long clientId, Long eventId, String reference) {
        Reservation reservation = this.reservationRepository.findByReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation " + reference + " not found"));
        PaymentService.checkOwnership(reservation, clientId, eventId, reference);
        return reservation;
    }

    private static void checkOwnership(Reservation reservation, Long clientId, Long eventId, String reference) {
        boolean belongsToClientAndEvent = reservation.getClient().getId().equals(clientId)
                && reservation.getReservationLines().stream()
                        .allMatch(reservationLine -> reservationLine.getEventTicketCategory().getEvent().getId().equals(eventId));

        if (!belongsToClientAndEvent)
            throw new ResourceNotFoundException("Reservation " + reference + " not found");
    }

    private Payment createPendingPayment(Reservation reservation) {
        Payment payment = Payment.builder()
                .idempotencyKey(this.generateUniqueIdempotencyKey())
                .reference(this.generateUniqueReference())
                .amount(reservation.getTotalAmount())
                .paymentDate(this.clock.instant())
                .status(PaymentStatus.PENDING)
                .reservation(reservation)
                .build();

        try {
            return this.paymentRepository.saveAndFlush(payment);
        } catch (DataIntegrityViolationException raceLost) {
            throw new SystemException("Could not store the payment", raceLost);
        }
    }

    private String generateUniqueIdempotencyKey() {
        for (int attempt = 0; attempt < PaymentService.MAX_ATTEMPTS; attempt++) {
            String idempotencyKey = CodeGenerator.randomCode(PaymentService.IDEMPOTENCY_PREFIX, PaymentService.CODE_LENGTH);
            if (!this.paymentRepository.existsByIdempotencyKey(idempotencyKey)) {
                return idempotencyKey;
            }
        }
        throw new SystemException("Could not generate a unique payment idempotency key");
    }

    private String generateUniqueReference() {
        for (int attempt = 0; attempt < PaymentService.MAX_ATTEMPTS; attempt++) {
            String reference = CodeGenerator.randomCode(PaymentService.REFERENCE_PREFIX, PaymentService.CODE_LENGTH);
            if (!this.paymentRepository.existsByReference(reference)) {
                return reference;
            }
        }
        throw new SystemException("Could not generate a unique payment reference");
    }
}
