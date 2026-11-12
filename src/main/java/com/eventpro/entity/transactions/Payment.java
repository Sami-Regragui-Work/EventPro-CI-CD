package com.eventpro.entity.transactions;

import com.eventpro.domain.enums.PaymentStatus;
import com.eventpro.entity.reservations.Reservation;
import com.eventpro.exception.conflict.IllegalPaymentStateException;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity @Table(name = "payments")
@Builder @AllArgsConstructor
@Getter @Setter @NoArgsConstructor
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String idempotencyKey;

    @Column(nullable = false, unique = true)
    private String reference;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private Instant paymentDate;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Setter(AccessLevel.NONE)
    private PaymentStatus status;

    @OneToOne(optional = false)
    private Reservation reservation;


    public void assertCanTransitionTo(PaymentStatus desiredStatus) {
        if (!this.status.canTransitionTo(desiredStatus)) {
            throw new IllegalPaymentStateException("Cannot change payment from %s to %s".formatted(this.status, desiredStatus));
        }
    }

    public void transitionTo(PaymentStatus desiredStatus) {
        this.assertCanTransitionTo(desiredStatus);
        this.status = desiredStatus;
    }
}