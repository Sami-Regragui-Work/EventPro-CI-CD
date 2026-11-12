package com.eventpro.repository;

import com.eventpro.entity.transactions.Refund;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundRepository extends JpaRepository<Refund, Long> {
}
