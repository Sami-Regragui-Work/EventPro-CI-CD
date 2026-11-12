package com.eventpro.repository;

import com.eventpro.entity.tickets.TicketCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TicketCategoryRepository extends JpaRepository<TicketCategory, Long> {
    Optional<TicketCategory> findByName(String name);
}
