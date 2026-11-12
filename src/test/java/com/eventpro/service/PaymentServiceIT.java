package com.eventpro.service;

import com.eventpro.TestcontainersConfiguration;
import com.eventpro.domain.enums.PaymentStatus;
import com.eventpro.domain.enums.ReservationStatus;
import com.eventpro.domain.enums.TicketStatus;
import com.eventpro.dto.request.payments.ConfirmPaymentRequest;
import com.eventpro.dto.request.reservations.CreateReservationRequest;
import com.eventpro.dto.request.reservations.ReservationLineRequest;
import com.eventpro.domain.enums.EventStatus;
import com.eventpro.entity.Event;
import com.eventpro.entity.reservations.Reservation;
import com.eventpro.entity.tickets.EventTicketCategory;
import com.eventpro.entity.tickets.Ticket;
import com.eventpro.entity.tickets.TicketCategory;
import com.eventpro.entity.transactions.Payment;
import com.eventpro.entity.users.Client;
import com.eventpro.entity.users.Organizer;
import com.eventpro.exception.badrequest.ValidationException;
import com.eventpro.exception.conflict.ReservationExpiredException;
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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class PaymentServiceIT {
    @Autowired PaymentService paymentService;
    @Autowired ReservationService reservationService;
    @Autowired ReservationRepository reservationRepository;
    @Autowired ReservationLineRepository reservationLineRepository;
    @Autowired EventTicketCategoryRepository eventTicketCategoryRepository;
    @Autowired EventRepository eventRepository;
    @Autowired OrganizerRepository organizerRepository;
    @Autowired ClientRepository clientRepository;
    @Autowired TicketCategoryRepository ticketCategoryRepository;
    @Autowired TicketRepository ticketRepository;
    @Autowired PaymentRepository paymentRepository;
    @Autowired RefundRepository refundRepository;
    @Autowired PromotionRepository promotionRepository;
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
        this.promotionRepository.deleteAllInBatch();
        this.clientRepository.deleteAllInBatch();
        this.organizerRepository.deleteAllInBatch();
    }

    @Test
    void confirmReplayReturnsSameResult() throws ValidationException {
        PaymentServiceIT.Fixture fixture = this.fixture("replay");

        Payment payment = this.paymentService.initiate(fixture.client().getId(), fixture.event().getId(),
                fixture.reservation().getReference());

        ConfirmPaymentRequest confirmRequest = new ConfirmPaymentRequest();
        confirmRequest.setIdempotencyKey(payment.getIdempotencyKey());

        Payment confirmedPayment = this.paymentService.confirm(fixture.client().getId(), fixture.event().getId(),
                fixture.reservation().getReference(), confirmRequest.getIdempotencyKey());
        Payment replayedPayment = this.paymentService.confirm(fixture.client().getId(), fixture.event().getId(),
                fixture.reservation().getReference(), confirmRequest.getIdempotencyKey());

        assertEquals(PaymentStatus.SUCCESS, confirmedPayment.getStatus());
        assertEquals(payment.getId(), replayedPayment.getId());
        assertEquals(payment.getIdempotencyKey(), replayedPayment.getIdempotencyKey());

        Reservation confirmedReservation = this.reservationRepository.findById(fixture.reservation().getId()).orElseThrow();
        assertEquals(ReservationStatus.CONFIRMED, confirmedReservation.getStatus());

        List<Ticket> issuedTickets = this.ticketRepository.findAll();
        assertEquals(1, issuedTickets.size());
        assertEquals(TicketStatus.VALID, issuedTickets.getFirst().getStatus());
    }

    @Test
    void confirmIssuesTicketsStampedFromTheClock() throws ValidationException {
        PaymentServiceIT.Fixture fixture = this.fixture("issueDate");

        Payment payment = this.paymentService.initiate(fixture.client().getId(), fixture.event().getId(),
                fixture.reservation().getReference());

        ConfirmPaymentRequest confirmRequest = new ConfirmPaymentRequest();
        confirmRequest.setIdempotencyKey(payment.getIdempotencyKey());

        Instant before = this.clock.instant().truncatedTo(ChronoUnit.MICROS);
        this.paymentService.confirm(fixture.client().getId(), fixture.event().getId(),
                fixture.reservation().getReference(), confirmRequest.getIdempotencyKey());
        Instant after = this.clock.instant().truncatedTo(ChronoUnit.MICROS);

        List<Ticket> issuedTickets = this.ticketRepository.findAll();
        assertEquals(1, issuedTickets.size());
        Ticket issuedTicket = issuedTickets.getFirst();
        assertTrue(!issuedTicket.getIssueDate().isBefore(before) && !issuedTicket.getIssueDate().isAfter(after),
                "issueDate " + issuedTicket.getIssueDate() + " outside [" + before + ", " + after + "]");
    }

    @Test
    void confirmAfterExpiryIsRejected() throws ValidationException {
        PaymentServiceIT.Fixture fixture = this.fixture("afterExpiry");

        Payment payment = this.paymentService.initiate(fixture.client().getId(), fixture.event().getId(),
                fixture.reservation().getReference());

        Reservation expiredReservation = this.reservationRepository.findById(fixture.reservation().getId()).orElseThrow();
        expiredReservation.setExpirationDate(this.clock.instant().minusSeconds(1));
        this.reservationRepository.save(expiredReservation);

        assertThrows(ReservationExpiredException.class, () -> this.paymentService.confirm(
                fixture.client().getId(), fixture.event().getId(),
                fixture.reservation().getReference(), payment.getIdempotencyKey()));

        Payment untouchedPayment = this.paymentRepository.findById(payment.getId()).orElseThrow();
        assertEquals(PaymentStatus.PENDING, untouchedPayment.getStatus());

        Reservation untouchedReservation = this.reservationRepository.findById(fixture.reservation().getId()).orElseThrow();
        assertEquals(ReservationStatus.PENDING_PAYMENT, untouchedReservation.getStatus());

        assertTrue(this.ticketRepository.findAll().isEmpty());
    }

    @Test
    void expiryMarksPendingPaymentFailed() throws ValidationException {
        PaymentServiceIT.Fixture fixture = this.fixture("expiryJob");

        Payment pendingPayment = this.paymentService.initiate(fixture.client().getId(), fixture.event().getId(),
                fixture.reservation().getReference());

        Reservation expiredReservation = this.reservationRepository.findById(fixture.reservation().getId()).orElseThrow();
        expiredReservation.setExpirationDate(this.clock.instant().minusSeconds(1));
        this.reservationRepository.save(expiredReservation);

        this.reservationService.expire(fixture.reservation().getId());

        Payment failedPayment = this.paymentRepository.findById(pendingPayment.getId()).orElseThrow();
        assertEquals(PaymentStatus.FAILED, failedPayment.getStatus());

        Reservation expired = this.reservationRepository.findById(fixture.reservation().getId()).orElseThrow();
        assertEquals(ReservationStatus.EXPIRED, expired.getStatus());
    }

    private PaymentServiceIT.Fixture fixture(String label) throws ValidationException {
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

        Instant now = this.clock.instant();
        Event event = this.eventRepository.save(Event.builder()
                .title("Event " + label)
                .location("Hall " + label)
                .startDate(now.plus(30, ChronoUnit.DAYS))
                .salesOpeningDate(now.minus(1, ChronoUnit.HOURS))
                .salesClosingDate(now.plus(1, ChronoUnit.HOURS))
                .capacity(100)
                .organizer(organizer)
                .build());
        event.transitionTo(EventStatus.PUBLISHED);
        this.eventRepository.save(event);

        TicketCategory ticketCategory = this.ticketCategoryRepository
                .save(TicketCategory.builder().name("Category_" + suffix.toUpperCase()).build());

        EventTicketCategory eventTicketCategory = this.eventTicketCategoryRepository.save(EventTicketCategory.builder()
                .unitPrice(new BigDecimal("50.00"))
                .totalQuantity(10)
                .maxQuotaPerClient(10)
                .event(event)
                .ticketCategory(ticketCategory)
                .build());

        ReservationLineRequest line = new ReservationLineRequest();
        line.setEventTicketCategoryId(eventTicketCategory.getId());
        line.setQuantity(1);

        CreateReservationRequest createRequest = new CreateReservationRequest();
        createRequest.setReservationLines(List.of(line));

        Reservation reservation = this.reservationService.create(client.getId(), event.getId(), createRequest);
        return new PaymentServiceIT.Fixture(suffix, event, reservation, client);
    }

    private record Fixture(String suffix, Event event, Reservation reservation, Client client) {}
}
