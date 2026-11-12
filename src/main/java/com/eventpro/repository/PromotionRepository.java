package com.eventpro.repository;

import com.eventpro.entity.transactions.Promotion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    List<Promotion> findByEventId(Long eventId);
    Optional<Promotion> findByIdAndEventId(Long id, Long eventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Promotion> findWithLockByEventIdAndCode(Long eventId, String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Promotion> findWithLockById(Long id);

    boolean existsByEventIdAndCode(Long eventId, String code);
}