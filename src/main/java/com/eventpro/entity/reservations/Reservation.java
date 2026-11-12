package com.eventpro.entity.reservations;

import com.eventpro.domain.enums.ReservationStatus;
import com.eventpro.entity.transactions.Payment;
import com.eventpro.entity.transactions.Promotion;
import com.eventpro.entity.users.Client;
import com.eventpro.exception.conflict.IllegalReservationStateException;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Entity @Table(name = "reservations")
@Builder @AllArgsConstructor
@Getter @Setter @NoArgsConstructor
public class Reservation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String reference;

    @Column(nullable = false)
    private Instant creationDate;

    @Column(nullable = false)
    private Instant expirationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Setter(AccessLevel.NONE)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.PENDING_PAYMENT;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    private Promotion promotion;

    @OneToMany(mappedBy = "reservation")
    private List<ReservationLine> reservationLines;

    @OneToOne(mappedBy = "reservation")
    private Payment payment;

    public boolean isExpiredAt(Instant now) {
        return this.status == ReservationStatus.PENDING_PAYMENT && !now.isBefore(this.expirationDate);
    }

    public void assertCanTransitionTo(ReservationStatus desiredStatus) {
        if (!this.status.canTransitionTo(desiredStatus)) {
            throw new IllegalReservationStateException("Cannot change reservation from %s to %s".formatted(this.status, desiredStatus));
        }
    }

    public void transitionTo(ReservationStatus desiredStatus) {
        this.assertCanTransitionTo(desiredStatus);
        this.status = desiredStatus;
    }
}