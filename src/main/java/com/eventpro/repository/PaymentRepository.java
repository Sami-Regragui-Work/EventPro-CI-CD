package com.eventpro.repository;

import com.eventpro.entity.transactions.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByReservationId(Long reservationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Payment> findWithLockByReservationId(Long reservationId);

    boolean existsByReference(String reference);

    boolean existsByIdempotencyKey(String idempotencyKey);
}
