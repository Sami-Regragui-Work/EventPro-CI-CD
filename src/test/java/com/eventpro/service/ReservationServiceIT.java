package com.eventpro.service;

import com.eventpro.TestcontainersConfiguration;
import com.eventpro.domain.enums.EventStatus;
import com.eventpro.domain.enums.ReservationStatus;
import com.eventpro.dto.request.reservations.CreateReservationRequest;
import com.eventpro.dto.request.reservations.ReservationLineRequest;
import com.eventpro.entity.Event;
import com.eventpro.entity.reservations.Reservation;
import com.eventpro.entity.tickets.EventTicketCategory;
import com.eventpro.entity.tickets.TicketCategory;
import com.eventpro.entity.users.Client;
import com.eventpro.entity.users.Organizer;
import com.eventpro.exception.badrequest.ValidationException;
import com.eventpro.exception.conflict.InsufficientStockException;
import com.eventpro.exception.conflict.QuotaExceededException;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ReservationServiceIT {
    @Autowired ReservationService reservationService;
    @Autowired ReservationRepository reservationRepository;
    @Autowired ReservationLineRepository reservationLineRepository;
    @Autowired EventTicketCategoryRepository eventTicketCategoryRepository;
    @Autowired EventRepository eventRepository;
    @Autowired OrganizerRepository organizerRepository;
    @Autowired ClientRepository clientRepository;
    @Autowired TicketCategoryRepository ticketCategoryRepository;
    @Autowired PromotionRepository promotionRepository;
    @Autowired PaymentRepository paymentRepository;
    @Autowired RefundRepository refundRepository;
    @Autowired TicketRepository ticketRepository;
    @Autowired Clock clock;

    @AfterEach
    void clearData() {
        this.ticketRepository.deleteAll();
        this.refundRepository.deleteAll();
        this.paymentRepository.deleteAll();
        this.reservationLineRepository.deleteAll();
        this.reservationRepository.deleteAll();
        this.eventTicketCategoryRepository.deleteAll();
        this.eventRepository.deleteAll();
        this.ticketCategoryRepository.deleteAll();
        this.promotionRepository.deleteAll();
        this.clientRepository.deleteAll();
        this.organizerRepository.deleteAll();
    }

    @Test
    void lastSeatGoesToOneReservation() throws Exception {
        ReservationServiceIT.Fixture fixture = this.fixture("lastSeat", 1, 1);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Future<ReservationServiceIT.CreateOutcome>> pendingOutcomes = new ArrayList<>();
        for (Client client : fixture.clients()) {
            pendingOutcomes.add(executor.submit((Callable<ReservationServiceIT.CreateOutcome>) () -> {
                try {
                    return new ReservationServiceIT.CreateOutcome(
                            this.reservationService.create(client.getId(), fixture.event().getId(),
                                    ReservationServiceIT.request(fixture.category().getId(), 1, null)), null);
                } catch (RuntimeException stockFailure) {
                    return new ReservationServiceIT.CreateOutcome(null, stockFailure);
                }
            }));
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));

        List<ReservationServiceIT.CreateOutcome> settledOutcomes = new ArrayList<>();
        for (Future<ReservationServiceIT.CreateOutcome> pendingOutcome : pendingOutcomes) {
            try {
                settledOutcomes.add(pendingOutcome.get());
            } catch (Exception executionFailure) {
                throw new IllegalStateException("Reservation creation thread died", executionFailure);
            }
        }

        long grantedReservations = settledOutcomes.stream().filter(outcome -> outcome.reservation() != null).count();
        assertEquals(1, grantedReservations);

        for (ReservationServiceIT.CreateOutcome settledOutcome : settledOutcomes) {
            if (settledOutcome.failure() != null) {
                assertInstanceOf(InsufficientStockException.class, settledOutcome.failure());
            }
        }
    }

    @Test
    void createStampsTheCreationDateFromTheClock() throws ValidationException {
        ReservationServiceIT.Fixture fixture = this.fixture("creationDate", 2, 2);

        Instant before = this.clock.instant().truncatedTo(ChronoUnit.MICROS);
        Reservation reservation = this.reservationService.create(fixture.clients().getFirst().getId(),
                fixture.event().getId(), ReservationServiceIT.request(fixture.category().getId(), 1, null));
        Instant after = this.clock.instant().truncatedTo(ChronoUnit.MICROS);

        assertEquals(ReservationStatus.PENDING_PAYMENT, reservation.getStatus());
        assertTrue(!reservation.getCreationDate().isBefore(before) && !reservation.getCreationDate().isAfter(after),
                "creationDate " + reservation.getCreationDate() + " outside [" + before + ", " + after + "]");
    }

    @Test
    void expiredReservationReleasesStock() throws ValidationException {
        ReservationServiceIT.Fixture fixture = this.fixture("expiry", 2, 2);
        Client client = fixture.clients().getFirst();

        try {
            Reservation expiredReservation = this.reservationService.create(client.getId(), fixture.event().getId(),
                    ReservationServiceIT.request(fixture.category().getId(), 2, null));
            expiredReservation.setExpirationDate(this.clock.instant().minusSeconds(1));
            this.reservationRepository.save(expiredReservation);
            this.reservationService.expire(expiredReservation.getId());

            assertEquals(ReservationStatus.EXPIRED,
                    this.reservationRepository.findById(expiredReservation.getId()).orElseThrow().getStatus());

            Client secondClient = this.clientRepository.save(Client.builder()
                    .name("Client expiry-two")
                    .email("client-two-" + fixture.suffix() + "@it.eventpro.dev")
                    .phone("+000000002")
                    .build());

            Reservation secondReservation = this.reservationService.create(secondClient.getId(), fixture.event().getId(),
                    ReservationServiceIT.request(fixture.category().getId(), 2, null));
            assertEquals(ReservationStatus.PENDING_PAYMENT, secondReservation.getStatus());
        } catch (ValidationException unexpected) {
            throw new IllegalStateException("Fixture reservation rejected unexpectedly", unexpected);
        }
    }

    @Test
    void quotaIsEnforcedPerClient() throws ValidationException {
        ReservationServiceIT.Fixture fixture = this.fixture("quota", 3, 2);
        Client client = fixture.clients().getFirst();

        this.reservationService.create(client.getId(), fixture.event().getId(),
                ReservationServiceIT.request(fixture.category().getId(), 2, null));

        assertThrows(QuotaExceededException.class, () -> {
            try {
                this.reservationService.create(client.getId(), fixture.event().getId(),
                        ReservationServiceIT.request(fixture.category().getId(), 1, null));
            } catch (ValidationException unexpected) {
                throw new IllegalStateException(unexpected);
            }
        });
    }

    private ReservationServiceIT.Fixture fixture(String label, int totalQuantity, int maxQuotaPerClient) {
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
                .totalQuantity(totalQuantity)
                .maxQuotaPerClient(maxQuotaPerClient)
                .event(event)
                .ticketCategory(ticketCategory)
                .build());

        return new ReservationServiceIT.Fixture(suffix, event, eventTicketCategory, List.of(client));
    }

    private static CreateReservationRequest request(Long eventTicketCategoryId, int quantity, String promotionCode) {
        ReservationLineRequest line = new ReservationLineRequest();
        line.setEventTicketCategoryId(eventTicketCategoryId);
        line.setQuantity(quantity);

        CreateReservationRequest createRequest = new CreateReservationRequest();
        createRequest.setPromotionCode(promotionCode);
        createRequest.setReservationLines(List.of(line));
        return createRequest;
    }

    private record Fixture(String suffix, Event event, EventTicketCategory category, List<Client> clients) {}

    private record CreateOutcome(Reservation reservation, RuntimeException failure) {}
}
