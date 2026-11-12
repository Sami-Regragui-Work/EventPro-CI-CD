package com.eventpro.entity.tickets;

import com.eventpro.entity.Event;
import com.eventpro.entity.reservations.ReservationLine;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Entity @Table(
        name = "event_ticket_categories",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_event_category",
                columnNames = {"event_id", "ticket_category_id"}
        )
)
@Builder @AllArgsConstructor
@Getter @Setter @NoArgsConstructor
public class EventTicketCategory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int totalQuantity;

    @Column(nullable = false)
    private int maxQuotaPerClient;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Event event;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private TicketCategory ticketCategory;

    @OneToMany(mappedBy = "eventTicketCategory")
    private List<ReservationLine> reservationLines;
}