package com.eventpro.repository;

import com.eventpro.domain.enums.ReservationStatus;
import com.eventpro.entity.reservations.Reservation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Reservation> findWithLockById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Reservation> findWithLockByReference(String reference);

    @Query("""
            SELECT DISTINCT reservation
            FROM Reservation reservation
            JOIN reservation.reservationLines reservationLine
            JOIN reservationLine.eventTicketCategory eventTicketCategory
            WHERE eventTicketCategory.event.id = :eventId
            AND reservation.status = :status
            """)
    List<Reservation> findByEventIdAndStatus(Long eventId, ReservationStatus status);

    @EntityGraph(attributePaths = {"promotion", "reservationLines"})
    Optional<Reservation> findByReference(String reference);

    boolean existsByReference(String reference);

    boolean existsByPromotionId(Long promotionId);

    @EntityGraph(attributePaths = "promotion")
    List<Reservation> findByStatusAndExpirationDateLessThanEqual(ReservationStatus status, Instant now);
}
