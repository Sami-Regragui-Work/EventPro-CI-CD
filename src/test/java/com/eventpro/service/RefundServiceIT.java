package com.eventpro.service;

import com.eventpro.TestcontainersConfiguration;
import com.eventpro.domain.CodeGenerator;
import com.eventpro.domain.enums.EventStatus;
import com.eventpro.domain.enums.PaymentStatus;
import com.eventpro.domain.enums.ReservationStatus;
import com.eventpro.domain.enums.TicketStatus;
import com.eventpro.entity.Event;
import com.eventpro.entity.reservations.Reservation;
import com.eventpro.entity.reservations.ReservationLine;
import com.eventpro.entity.tickets.EventTicketCategory;
import com.eventpro.entity.tickets.Ticket;
import com.eventpro.entity.tickets.TicketCategory;
import com.eventpro.entity.transactions.Payment;
import com.eventpro.entity.transactions.Refund;
import com.eventpro.entity.users.Client;
import com.eventpro.entity.users.Organizer;
import com.eventpro.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class RefundServiceIT {
    @Autowired RefundService refundService;
    @Autowired RefundRepository refundRepository;
    @Autowired TicketRepository ticketRepository;
    @Autowired PaymentRepository paymentRepository;
    @Autowired ReservationLineRepository reservationLineRepository;
    @Autowired ReservationRepository reservationRepository;
    @Autowired EventTicketCategoryRepository eventTicketCategoryRepository;
    @Autowired EventRepository eventRepository;
    @Autowired TicketCategoryRepository ticketCategoryRepository;
    @Autowired ClientRepository clientRepository;
    @Autowired OrganizerRepository organizerRepository;
    @Autowired Clock clock;

    @AfterEach
    void clearData() {
        this.refundRepository.deleteAllInBatch();
        this.ticketRepository.deleteAllInBatch();
        this.paymentRepository.deleteAllInBatch();
        this.reservationLineRepository.deleteAllInBatch();
        this.reservationRepository.deleteAllInBatch();
        this.eventTicketCategoryRepository.deleteAllInBatch();
        this.eventRepository.deleteAllInBatch();
        this.ticketCategoryRepository.deleteAllInBatch();
        this.clientRepository.deleteAllInBatch();
        this.organizerRepository.deleteAllInBatch();
    }

    @Test
    void refundStampsTheRequestDateFromTheClock() {
        RefundServiceIT.Fixture fixture = this.fixture("refundDate");

        Instant before = this.clock.instant().truncatedTo(ChronoUnit.MICROS);
        this.refundService.refundConfirmedReservation(fixture.reservation(), this.clock.instant());
        Instant after = this.clock.instant().truncatedTo(ChronoUnit.MICROS);

        Refund refund = this.refundRepository.findAll().getFirst();
        assertTrue(!refund.getRequestDate().isBefore(before) && !refund.getRequestDate().isAfter(after),
                "requestDate " + refund.getRequestDate() + " outside [" + before + ", " + after + "]");
    }

    @Test
    void refundCancelsValidTicketsButKeepsUsedOnes() {
        RefundServiceIT.Fixture fixture = this.fixture("refund-used");

        this.refundService.refundConfirmedReservation(fixture.reservation(), this.clock.instant());

        Reservation refunded = this.reservationRepository.findById(fixture.reservation().getId()).orElseThrow();
        assertEquals(ReservationStatus.CANCELED, refunded.getStatus());

        Ticket validTicket = this.ticketRepository.findById(fixture.validTicket().getId()).orElseThrow();
        assertEquals(TicketStatus.CANCELED, validTicket.getStatus());

        Ticket usedTicket = this.ticketRepository.findById(fixture.usedTicket().getId()).orElseThrow();
        assertEquals(TicketStatus.USED, usedTicket.getStatus());
    }

    private RefundServiceIT.Fixture fixture(String label) {
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

        Ticket validTicket = this.ticketRepository.save(Ticket.builder()
                .code(this.uniqueTicketCode())
                .issueDate(this.clock.instant())
                .status(TicketStatus.VALID)
                .reservationLine(reservationLine)
                .build());

        Ticket usedTicket = this.ticketRepository.save(Ticket.builder()
                .code(this.uniqueTicketCode())
                .issueDate(this.clock.instant())
                .scanDate(this.clock.instant())
                .status(TicketStatus.USED)
                .reservationLine(reservationLine)
                .build());

        this.paymentRepository.save(Payment.builder()
                .idempotencyKey("IDM-" + CodeGenerator.randomCode("", 10))
                .reference("PAY-" + CodeGenerator.randomCode("", 10))
                .amount(new BigDecimal("100.00"))
                .paymentDate(this.clock.instant())
                .status(PaymentStatus.SUCCESS)
                .reservation(reservation)
                .build());

        reservation.transitionTo(ReservationStatus.CONFIRMED);
        this.reservationRepository.save(reservation);

        return new RefundServiceIT.Fixture(reservation, validTicket, usedTicket);
    }

    private String uniqueTicketCode() {
        for (int attempt = 0; attempt < 5; attempt++) {
            String candidateCode = CodeGenerator.randomCode("TKT-", 10);
            if (!this.ticketRepository.existsByCode(candidateCode))
                return candidateCode;
        }
        throw new IllegalStateException("Could not generate a unique ticket code");
    }

    private record Fixture(Reservation reservation, Ticket validTicket, Ticket usedTicket) {}
}
