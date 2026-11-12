package com.eventpro.service;

import com.eventpro.domain.enums.PaymentStatus;
import com.eventpro.domain.enums.RefundStatus;
import com.eventpro.domain.enums.ReservationStatus;
import com.eventpro.domain.enums.TicketStatus;
import com.eventpro.entity.reservations.Reservation;
import com.eventpro.entity.reservations.ReservationLine;
import com.eventpro.entity.tickets.Ticket;
import com.eventpro.entity.transactions.Payment;
import com.eventpro.entity.transactions.Promotion;
import com.eventpro.entity.transactions.Refund;
import com.eventpro.exception.internalservererror.SystemException;
import com.eventpro.exception.notfound.ResourceNotFoundException;
import com.eventpro.repository.PromotionRepository;
import com.eventpro.repository.RefundRepository;
import com.eventpro.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class RefundService {
    private final RefundRepository repository;
    private final ReservationRepository reservationRepository;
    private final PromotionRepository promotionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void refundConfirmedReservation(Reservation reservation, Instant now) {
        Reservation lockedReservation = this.lockReservation(reservation.getId());
        if (lockedReservation.getStatus() != ReservationStatus.CONFIRMED)
            return;

        Payment payment = lockedReservation.getPayment();
        if (payment == null)
            throw new SystemException("Confirmed reservation " + lockedReservation.getReference() + " has no payment");

        Refund refund = Refund.builder()
                .amount(lockedReservation.getTotalAmount())
                .reason("Reservation " + lockedReservation.getReference() + " cancelled")
                .requestDate(now)
                .status(RefundStatus.APPROVED)
                .payment(payment)
                .build();
        this.repository.save(refund);

        for (ReservationLine reservationLine : lockedReservation.getReservationLines()) {
            for (Ticket ticket : reservationLine.getTickets()) {
                // Only unused tickets come back into the pool: an already scanned ticket
                // stays USED (the holder attended), and a canceled one needs no change.
                if (ticket.getStatus() == TicketStatus.VALID) {
                    ticket.transitionTo(TicketStatus.CANCELED);
                }
            }
        }

        lockedReservation.transitionTo(ReservationStatus.CANCELED);
        this.releasePromotionUsage(lockedReservation);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancelPendingReservation(Reservation reservation) {
        Reservation lockedReservation = this.lockReservation(reservation.getId());
        if (lockedReservation.getStatus() != ReservationStatus.PENDING_PAYMENT)
            return;

        lockedReservation.transitionTo(ReservationStatus.CANCELED);

        Payment pendingPayment = lockedReservation.getPayment();
        if (pendingPayment != null && pendingPayment.getStatus() == PaymentStatus.PENDING)
            pendingPayment.transitionTo(PaymentStatus.FAILED);

        this.releasePromotionUsage(lockedReservation);
    }

    private Reservation lockReservation(Long reservationId) {
        return this.reservationRepository.findWithLockById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation " + reservationId + " not found"));
    }

    private void releasePromotionUsage(Reservation reservation) {
        Promotion appliedPromotion = reservation.getPromotion();

        if (appliedPromotion == null)
            return;

        Promotion lockedPromotion = this.promotionRepository.findWithLockById(appliedPromotion.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Promotion " + appliedPromotion.getId() + " not found"));
        lockedPromotion.releaseUsage();
    }
}
