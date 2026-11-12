package com.eventpro.service;

import com.eventpro.entity.tickets.Ticket;
import com.eventpro.entity.users.ControlAgent;
import com.eventpro.exception.notfound.ResourceNotFoundException;
import com.eventpro.repository.ControlAgentRepository;
import com.eventpro.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class CheckInService {
    private final TicketRepository repository;
    private final ControlAgentRepository controlAgentRepository;
    private final Clock clock;

    public Ticket scan(Long agentId, String ticketCode) {
        ControlAgent agent = this.controlAgentRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent " + agentId + " not found"));
        Ticket ticket = this.repository.findWithLockByCode(ticketCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket " + ticketCode + " not found"));

        ticket.scan(this.clock.instant());
        return ticket;
    }
}
