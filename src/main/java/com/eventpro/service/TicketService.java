package com.eventpro.service;

import com.eventpro.domain.CodeGenerator;
import com.eventpro.domain.enums.TicketStatus;
import com.eventpro.entity.reservations.Reservation;
import com.eventpro.entity.reservations.ReservationLine;
import com.eventpro.entity.tickets.Ticket;
import com.eventpro.exception.internalservererror.SystemException;
import com.eventpro.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class TicketService {
    private static final String CODE_PREFIX = "TKT-";
    private static final int CODE_LENGTH = 10;
    private static final int MAX_ATTEMPTS = 5;

    private final TicketRepository repository;

    public List<Ticket> issueTickets(Reservation reservation, Instant now) {
        List<Ticket> tickets = new ArrayList<>();

        for (ReservationLine reservationLine : reservation.getReservationLines()) {
            for (int unitNumber = 0; unitNumber < reservationLine.getQuantity(); unitNumber++) {
                tickets.add(Ticket.builder()
                        .code(this.generateUniqueCode())
                        .issueDate(now)
                        .status(TicketStatus.VALID)
                        .reservationLine(reservationLine)
                        .build());
            }
        }

        return this.repository.saveAll(tickets);
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < TicketService.MAX_ATTEMPTS; attempt++) {
            String code = CodeGenerator.randomCode(TicketService.CODE_PREFIX, TicketService.CODE_LENGTH);
            if (!this.repository.existsByCode(code)) {
                return code;
            }
        }
        throw new SystemException("Could not generate a unique ticket code");
    }
}
