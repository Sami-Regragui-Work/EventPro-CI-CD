package com.eventpro.repository;

import com.eventpro.entity.tickets.EventTicketCategory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EventTicketCategoryRepository extends JpaRepository<EventTicketCategory, Long> {
    interface EventTotalSum {
        Long getEventId();
        Long getTotalQuantity();
    }

    @Query("""
        SELECT etc.event.id AS eventId,
               COALESCE(SUM(etc.totalQuantity), 0) AS totalQuantity
        FROM EventTicketCategory etc
        WHERE etc.event.id IN :eventIds
        GROUP BY etc.event.id
        """)
    List<EventTotalSum> sumTotalByEventIds(@Param("eventIds") Collection<Long> eventIds);

    @EntityGraph(attributePaths = "ticketCategory")
    List<EventTicketCategory> findByEventId(Long eventId);

    @EntityGraph(attributePaths = "ticketCategory")
    Optional<EventTicketCategory> findByIdAndEventId(Long id, Long eventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EventTicketCategory> findWithLockById(Long id);

    boolean existsByEventIdAndTicketCategoryId(Long eventId, Long ticketCategoryId);

    @Query("""
        SELECT COALESCE(SUM(etc.totalQuantity), 0)
        FROM EventTicketCategory etc
        WHERE etc.event.id = :eventId
        """)
    long sumTotalQuantityByEventId(@Param("eventId") Long eventId);

    long countByEventId(Long eventId);
}
