package com.eventpro.repository;

import com.eventpro.entity.users.Client;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, Long> {
}