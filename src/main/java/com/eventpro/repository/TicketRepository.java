package com.eventpro.repository;

import com.eventpro.entity.tickets.Ticket;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;public interface TicketRepository extends JpaRepository<Ticket, Long> {
    boolean existsByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Ticket> findWithLockByCode(String code);
}
