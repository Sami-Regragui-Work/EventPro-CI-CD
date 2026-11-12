package com.eventpro.controller;

import com.eventpro.TestcontainersConfiguration;
import com.eventpro.domain.enums.EventStatus;
import com.eventpro.domain.enums.ReservationStatus;
import com.eventpro.dto.request.reservations.CreateReservationRequest;
import com.eventpro.dto.request.reservations.ReservationLineRequest;
import com.eventpro.entity.Event;
import com.eventpro.entity.users.Client;
import com.eventpro.entity.users.Organizer;
import com.eventpro.exception.badrequest.ValidationException;
import com.eventpro.repository.*;
import com.eventpro.service.ReservationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-application test for the client cancel endpoint. The refund commits in a
 * separate transaction, and the endpoint re-reads inside the same request: this
 * exercises the shared persistence context and would previously answer with the
 * cached pre-cancel status.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ReservationCancelEndpointIT {
    @Autowired MockMvc mockMvc;
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
    void cancelResponseReportsTheCommittedStatus() throws Exception {
        String suffix = "cancel-" + UUID.randomUUID();

        Organizer organizer = this.organizerRepository.save(Organizer.builder()
                .name("Organizer cancel")
                .email("organizer-" + suffix + "@it.eventpro.dev")
                .organisationName("Organisation cancel")
                .build());

        Client client = this.clientRepository.save(Client.builder()
                .name("Client cancel")
                .email("client-" + suffix + "@it.eventpro.dev")
                .phone("+000000001")
                .build());

        Instant now = this.clock.instant();
        Event event = this.eventRepository.save(Event.builder()
                .title("Event cancel")
                .location("Hall cancel")
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

        ReservationLineRequest line = new ReservationLineRequest();
        line.setEventTicketCategoryId(eventTicketCategory.getId());
        line.setQuantity(1);
        CreateReservationRequest createRequest = new CreateReservationRequest();
        createRequest.setReservationLines(List.of(line));

        var reservation = this.reservationService.create(client.getId(), event.getId(), createRequest);
        assertEquals(ReservationStatus.PENDING_PAYMENT, reservation.getStatus());

        this.mockMvc.perform(post("/api/clients/{clientId}/events/{eventId}/reservations/{reference}/cancel",
                        client.getId(), event.getId(), reservation.getReference()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"))
                .andExpect(jsonPath("$.reference").value(reservation.getReference()));

        assertEquals(ReservationStatus.CANCELED,
                this.reservationRepository.findByReference(reservation.getReference()).orElseThrow().getStatus());
    }
}
