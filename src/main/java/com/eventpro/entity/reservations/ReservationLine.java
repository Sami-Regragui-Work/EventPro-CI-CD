package com.eventpro.entity.reservations;

import com.eventpro.entity.tickets.Ticket;
import com.eventpro.entity.tickets.EventTicketCategory;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Entity @Table(name = "reservation_lines", indexes = {
        @Index(name = "ix_reservation_lines_reservation_id", columnList = "reservation_id"),
        @Index(name = "ix_reservation_lines_event_ticket_category_id", columnList = "event_ticket_category_id"),
})
@Builder @AllArgsConstructor
@Getter @Setter @NoArgsConstructor
public class ReservationLine {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private BigDecimal fixedUnitPrice;

    @Column(nullable = false)
    private BigDecimal subtotal;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Reservation reservation;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private EventTicketCategory eventTicketCategory;

    @OneToMany(mappedBy = "reservationLine")
    // don't forget to enforce at least 1
    private List<Ticket> tickets;
}