package com.eventpro.repository;

import com.eventpro.domain.enums.ReservationStatus;
import com.eventpro.entity.reservations.ReservationLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ReservationLineRepository extends JpaRepository<ReservationLine, Long> {
    interface CategoryStockSum {
        Long getEventTicketCategoryId();
        Long getReservedQuantity();
        Long getSoldQuantity();
    }

    interface EventStockSum {
        Long getEventId();
        Long getCommittedQuantity();
    }

    @Query("""
        SELECT rl.eventTicketCategory.id AS eventTicketCategoryId,
               COALESCE(SUM(CASE WHEN rl.reservation.status = :pending THEN rl.quantity ELSE 0 END), 0) AS reservedQuantity,
               COALESCE(SUM(CASE WHEN rl.reservation.status = :confirmed THEN rl.quantity ELSE 0 END), 0) AS soldQuantity
        FROM ReservationLine rl
        WHERE rl.eventTicketCategory.event.id = :eventId
        AND rl.reservation.status IN :statuses
        GROUP BY rl.eventTicketCategory.id
        """)
    List<CategoryStockSum> sumStockByEventId(
            @Param("eventId") Long eventId,
            @Param("pending") ReservationStatus pending,
            @Param("confirmed") ReservationStatus confirmed,
            @Param("statuses") Collection<ReservationStatus> statuses
    );

    @Query("""
        SELECT rl.eventTicketCategory.event.id AS eventId,
               COALESCE(SUM(rl.quantity), 0) AS committedQuantity
        FROM ReservationLine rl
        WHERE rl.eventTicketCategory.event.id IN :eventIds
        AND rl.reservation.status IN :statuses
        GROUP BY rl.eventTicketCategory.event.id
        """)
    List<EventStockSum> sumCommittedByEventIds(
            @Param("eventIds") Collection<Long> eventIds,
            @Param("statuses") Collection<ReservationStatus> statuses
    );

    boolean existsByEventTicketCategoryId(Long eventTicketCategoryId);

    @Query("""
            SELECT COALESCE(SUM(rl.quantity), 0)
            FROM ReservationLine rl
            WHERE rl.eventTicketCategory.id = :eventTicketCategoryId
            AND rl.reservation.status in :statuses
            """)
    long sumQuantityByEventTicketCategoryIdAndReservationStatuses(
            @Param("eventTicketCategoryId") Long eventTicketCategoryId,
            @Param("statuses") Collection<ReservationStatus> statuses
            );

    @Query("""
            SELECT COALESCE(SUM(rl.quantity), 0)
            FROM ReservationLine rl
            WHERE rl.eventTicketCategory.id = :eventTicketCategoryId
            AND rl.reservation.client.id = :clientId
            AND rl.reservation.status IN :statuses
            """)
    long sumQuantityByEventTicketCategoryIdAndClientIdAndReservationStatuses(
            @Param("eventTicketCategoryId") Long eventTicketCategoryId,
            @Param("clientId") Long clientId,
            @Param("statuses") Collection<ReservationStatus> statuses
    );

    @Query("""
            SELECT rl.eventTicketCategory.event.id AS eventId,
                   rl.eventTicketCategory.event.title AS title,
                   COALESCE(SUM(rl.quantity), 0) AS ticketsSold,
                   COALESCE(SUM(rl.subtotal), 0) AS revenue
            FROM ReservationLine rl
            WHERE rl.eventTicketCategory.event.organizer.id = :organizerId
            AND rl.reservation.status = com.eventpro.domain.enums.ReservationStatus.CONFIRMED
            GROUP BY rl.eventTicketCategory.event.id, rl.eventTicketCategory.event.title
            """)
    List<EventSalesProjection> sumSalesByOrganizerId(Long organizerId);
}
