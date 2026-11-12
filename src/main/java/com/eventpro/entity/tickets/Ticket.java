package com.eventpro.entity.tickets;

import com.eventpro.domain.enums.TicketStatus;
import com.eventpro.entity.reservations.ReservationLine;
import com.eventpro.exception.conflict.IllegalTicketStateException;
import com.eventpro.exception.conflict.TicketAlreadyUsedException;
import com.eventpro.exception.conflict.TicketCancelledException;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity @Table(name = "tickets")
@Builder @AllArgsConstructor
@Getter @Setter @NoArgsConstructor
public class Ticket {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private Instant issueDate;

    private Instant scanDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Setter(AccessLevel.NONE)
    private TicketStatus status;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private ReservationLine reservationLine;

    public void assertCanTransitionTo(TicketStatus desiredStatus) {
        if (!this.status.canTransitionTo(desiredStatus)) {
            throw new IllegalTicketStateException("Cannot change ticket from %s to %s".formatted(this.status, desiredStatus));
        }
    }

    public void transitionTo(TicketStatus desiredStatus) {
        this.assertCanTransitionTo(desiredStatus);
        this.status = desiredStatus;
    }

    public void scan(Instant now) {
        switch (this.status) {
            case VALID -> {
                this.transitionTo(TicketStatus.USED);
                this.scanDate = now;
            }
            case USED -> throw new TicketAlreadyUsedException();
            case CANCELED -> throw new TicketCancelledException();
        }
    }
}