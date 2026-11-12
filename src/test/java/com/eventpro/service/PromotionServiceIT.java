package com.eventpro.service;

import com.eventpro.TestcontainersConfiguration;
import com.eventpro.domain.enums.EventStatus;
import com.eventpro.domain.enums.ReservationStatus;
import com.eventpro.dto.request.CreatePromotionRequest;
import com.eventpro.dto.request.reservations.CreateReservationRequest;
import com.eventpro.dto.request.reservations.ReservationLineRequest;
import com.eventpro.domain.enums.DiscountType;
import com.eventpro.entity.Event;
import com.eventpro.entity.reservations.Reservation;
import com.eventpro.entity.transactions.Promotion;
import com.eventpro.entity.users.Client;
import com.eventpro.entity.users.Organizer;
import com.eventpro.exception.badrequest.ValidationException;
import com.eventpro.exception.conflict.PromotionInUseException;
import com.eventpro.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class PromotionServiceIT {
    @Autowired PromotionService promotionService;
    @Autowired ReservationService reservationService;
    @Autowired PromotionRepository promotionRepository;
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
    @Autowired Clock clock;

    @AfterEach
    void clearData() {
        this.refundRepository.deleteAllInBatch();
        this.ticketRepository.deleteAllInBatch();
        this.paymentRepository.deleteAllInBatch();
        this.reservationLineRepository.deleteAllInBatch();
        this.reservationRepository.deleteAllInBatch();
        this.eventTicketCategoryRepository.deleteAllInBatch();
        this.promotionRepository.deleteAllInBatch();
        this.eventRepository.deleteAllInBatch();
        this.clientRepository.deleteAllInBatch();
        this.organizerRepository.deleteAllInBatch();
    }

    @Test
    void deleteIsRefusedWhenACanceledReservationStillReferencesIt() throws ValidationException {
        PromotionServiceIT.Fixture fixture = this.fixture("spent-promo");

        CreateReservationRequest request = new CreateReservationRequest();
        request.setPromotionCode(fixture.promotion().getCode());
        ReservationLineRequest line = new ReservationLineRequest();
        line.setEventTicketCategoryId(fixture.eventTicketCategory().getId());
        line.setQuantity(1);
        request.setReservationLines(List.of(line));

        Reservation reservation = this.reservationService.create(
                fixture.client().getId(), fixture.event().getId(), request);
        assertEquals(ReservationStatus.PENDING_PAYMENT, reservation.getStatus());

        // Carrying the reservation to cancellation releases the usage counter but keeps
        // the promotion_id reference (audit history).
        this.reservationService.cancelByClient(fixture.client().getId(), fixture.event().getId(),
                reservation.getReference());

        Promotion spentPromotion = this.promotionRepository.findById(fixture.promotion().getId()).orElseThrow();
        assertEquals(0, spentPromotion.getUsageCount());

        assertThrows(PromotionInUseException.class, () -> this.promotionService.delete(
                fixture.organizer().getId(), fixture.event().getId(), fixture.promotion().getId()));
    }

    @Test
    void deleteSucceedsForAPromotionWithoutAnyReservation() throws ValidationException {
        PromotionServiceIT.Fixture fixture = this.fixture("fresh-promo");

        this.promotionService.delete(fixture.organizer().getId(), fixture.event().getId(), fixture.promotion().getId());

        assertEquals(false, this.promotionRepository.existsById(fixture.promotion().getId()));
    }

    private PromotionServiceIT.Fixture fixture(String label) throws ValidationException {
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

        var ticketCategory = this.ticketCategoryRepository
                .save(com.eventpro.entity.tickets.TicketCategory.builder().name("Category_" + suffix.toUpperCase()).build());

        var eventTicketCategory = this.eventTicketCategoryRepository.save(com.eventpro.entity.tickets.EventTicketCategory.builder()
                .unitPrice(new BigDecimal("50.00"))
                .totalQuantity(10)
                .maxQuotaPerClient(10)
                .event(event)
                .ticketCategory(ticketCategory)
                .build());

        CreatePromotionRequest promotionRequest = new CreatePromotionRequest();
        promotionRequest.setDiscountType(DiscountType.PERCENTAGE);
        promotionRequest.setDiscountValue(new BigDecimal("10"));
        promotionRequest.setStartDate(LocalDate.now(this.clock));
        promotionRequest.setEndDate(LocalDate.now(this.clock).plusDays(10));
        promotionRequest.setUsageLimit(10);
        Promotion promotion = this.promotionService.create(organizer.getId(), event.getId(), promotionRequest);

        return new PromotionServiceIT.Fixture(suffix, organizer, client, event, eventTicketCategory, promotion);
    }

    private record Fixture(String suffix, Organizer organizer, Client client, Event event,
                           com.eventpro.entity.tickets.EventTicketCategory eventTicketCategory,
                           Promotion promotion) {}
}
