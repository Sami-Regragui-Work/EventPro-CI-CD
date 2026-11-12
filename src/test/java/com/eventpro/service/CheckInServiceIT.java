package com.eventpro.service;

import com.eventpro.TestcontainersConfiguration;
import com.eventpro.domain.CodeGenerator;
import com.eventpro.domain.enums.EventStatus;
import com.eventpro.domain.enums.TicketStatus;
import com.eventpro.entity.Event;
import com.eventpro.entity.reservations.Reservation;
import com.eventpro.entity.reservations.ReservationLine;
import com.eventpro.entity.tickets.EventTicketCategory;
import com.eventpro.entity.tickets.Ticket;
import com.eventpro.entity.tickets.TicketCategory;
import com.eventpro.entity.users.Client;
import com.eventpro.entity.users.ControlAgent;
import com.eventpro.entity.users.Organizer;
import com.eventpro.exception.conflict.TicketAlreadyUsedException;
import com.eventpro.exception.notfound.ResourceNotFoundException;
import com.eventpro.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class CheckInServiceIT {
    @Autowired CheckInService checkInService;
    @Autowired TicketRepository ticketRepository;
    @Autowired ReservationLineRepository reservationLineRepository;
    @Autowired ReservationRepository reservationRepository;
    @Autowired EventTicketCategoryRepository eventTicketCategoryRepository;
    @Autowired EventRepository eventRepository;
    @Autowired OrganizerRepository organizerRepository;
    @Autowired ClientRepository clientRepository;
    @Autowired ControlAgentRepository controlAgentRepository;
    @Autowired TicketCategoryRepository ticketCategoryRepository;
    @Autowired PaymentRepository paymentRepository;
    @Autowired RefundRepository refundRepository;
    @Autowired PromotionRepository promotionRepository;
    @Autowired Clock clock;

    @AfterEach
    void clearData() {
        this.ticketRepository.deleteAllInBatch();
        this.refundRepository.deleteAllInBatch();
        this.paymentRepository.deleteAllInBatch();
        this.reservationLineRepository.deleteAllInBatch();
        this.reservationRepository.deleteAllInBatch();
        this.eventTicketCategoryRepository.deleteAllInBatch();
        this.eventRepository.deleteAllInBatch();
        this.ticketCategoryRepository.deleteAllInBatch();
        this.promotionRepository.deleteAllInBatch();
        this.clientRepository.deleteAllInBatch();
        this.organizerRepository.deleteAllInBatch();
        this.controlAgentRepository.deleteAllInBatch();
    }

    @Test
    void concurrentScansOnlyOneSucceeds() throws Exception {
        CheckInServiceIT.Fixture fixture = this.fixture("scanRace", 2);
        ControlAgent agent = this.agent("scanRace");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Future<CheckInServiceIT.ScanOutcome>> pendingOutcomes = new ArrayList<>();
        for (int scannerNumber = 0; scannerNumber < 2; scannerNumber++) {
            pendingOutcomes.add(executor.submit((Callable<CheckInServiceIT.ScanOutcome>) () -> {
                try {
                    return new CheckInServiceIT.ScanOutcome(
                            this.checkInService.scan(agent.getId(), fixture.tickets().getFirst().getCode()), null);
                } catch (RuntimeException scanFailure) {
                    return new CheckInServiceIT.ScanOutcome(null, scanFailure);
                }
            }));
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));

        List<CheckInServiceIT.ScanOutcome> settledOutcomes = new ArrayList<>();
        for (Future<CheckInServiceIT.ScanOutcome> pendingOutcome : pendingOutcomes) {
            settledOutcomes.add(pendingOutcome.get());
        }

        long successfulScans = settledOutcomes.stream().filter(outcome -> outcome.scannedTicket() != null).count();
        assertEquals(1, successfulScans);

        for (CheckInServiceIT.ScanOutcome settledOutcome : settledOutcomes) {
            if (settledOutcome.failure() != null) {
                assertInstanceOf(TicketAlreadyUsedException.class, settledOutcome.failure());
            }
        }

        Ticket scannedTicket = this.ticketRepository.findById(fixture.tickets().getFirst().getId()).orElseThrow();
        assertEquals(TicketStatus.USED, scannedTicket.getStatus());
        assertNotNull(scannedTicket.getScanDate());
    }

    @Test
    void unknownCodeIsAMiss() {
        ControlAgent agent = this.agent("unknownCode");
        assertThrows(ResourceNotFoundException.class,
                () -> this.checkInService.scan(agent.getId(), "TKT-" + CodeGenerator.randomCode("", 10)));
    }

    @Test
    void unknownAgentLeavesTicketUntouched() {
        CheckInServiceIT.Fixture fixture = this.fixture("unknownAgent", 1);
        Ticket ticket = fixture.tickets().getFirst();

        ResourceNotFoundException failure = assertThrows(ResourceNotFoundException.class,
                () -> this.checkInService.scan(Long.MAX_VALUE, ticket.getCode()));
        assertEquals("Agent " + Long.MAX_VALUE + " not found", failure.getMessage());

        Ticket reloadedTicket = this.ticketRepository.findById(ticket.getId()).orElseThrow();
        assertEquals(TicketStatus.VALID, reloadedTicket.getStatus());
        assertNull(reloadedTicket.getScanDate());
    }

    private CheckInServiceIT.Fixture fixture(String label, int ticketCount) {
        String suffix = label + "-" + UUID.randomUUID();

        Organizer organizer = this.organizerRepository.save(Organizer.builder()
                .name("Organizer " + label)
                .email("organizer-" + suffix + "@it.eventpro.dev")
                .organisationName("Organisation " + label)
                .build());

        Client client = this.clientRepository.save(Client.builder()
                .name("Client " + label)
                .email("client-" + suffix + "@it.eventpro.dev")
                .phone("+000000001")
                .build());

        Event event = this.eventRepository.save(Event.builder()
                .title("Event " + label)
                .location("Hall " + label)
                .startDate(this.clock.instant().plusSeconds(86_400))
                .salesOpeningDate(this.clock.instant().minusSeconds(3_600))
                .salesClosingDate(this.clock.instant().plusSeconds(3_600))
                .capacity(100)
                .organizer(organizer)
                .build());
        event.transitionTo(EventStatus.PUBLISHED);
        this.eventRepository.save(event);

        TicketCategory ticketCategory = this.ticketCategoryRepository
                .save(TicketCategory.builder().name("Category_" + suffix.toUpperCase()).build());

        EventTicketCategory eventTicketCategory = this.eventTicketCategoryRepository.save(EventTicketCategory.builder()
                .unitPrice(new BigDecimal("50.00"))
                .totalQuantity(50)
                .maxQuotaPerClient(50)
                .event(event)
                .ticketCategory(ticketCategory)
                .build());

        Reservation reservation = this.reservationRepository.save(Reservation.builder()
                .reference("RSV-" + CodeGenerator.randomCode("", 10))
                .creationDate(this.clock.instant())
                .expirationDate(this.clock.instant().plusSeconds(600))
                .totalAmount(new BigDecimal("100.00"))
                .client(client)
                .build());

        ReservationLine reservationLine = this.reservationLineRepository.save(ReservationLine.builder()
                .quantity(2)
                .fixedUnitPrice(new BigDecimal("50.00"))
                .subtotal(new BigDecimal("100.00"))
                .reservation(reservation)
                .eventTicketCategory(eventTicketCategory)
                .build());

        List<Ticket> tickets = new ArrayList<>();
        for (int ticketNumber = 0; ticketNumber < ticketCount; ticketNumber++) {
            tickets.add(this.ticketRepository.save(Ticket.builder()
                    .code(this.uniqueTicketCode())
                    .issueDate(this.clock.instant())
                    .status(TicketStatus.VALID)
                    .reservationLine(reservationLine)
                    .build()));
        }

        return new CheckInServiceIT.Fixture(suffix, reservation, reservationLine, tickets);
    }

    private ControlAgent agent(String label) {
        String suffix = label + "-" + UUID.randomUUID();
        return this.controlAgentRepository.save(ControlAgent.builder()
                .name("Agent " + label)
                .email("agent-" + suffix + "@it.eventpro.dev")
                .build());
    }

    private String uniqueTicketCode() {
        for (int attempt = 0; attempt < 5; attempt++) {
            String candidateCode = CodeGenerator.randomCode("TKT-", 10);
            if (!this.ticketRepository.existsByCode(candidateCode))
                return candidateCode;
        }
        throw new IllegalStateException("could not find a free ticket code");
    }

    private record Fixture(String suffix, Reservation reservation, ReservationLine line, List<Ticket> tickets) {}

    private record ScanOutcome(Ticket scannedTicket, RuntimeException failure) {}
}