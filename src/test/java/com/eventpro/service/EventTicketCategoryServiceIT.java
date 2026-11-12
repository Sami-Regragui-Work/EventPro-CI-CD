package com.eventpro.service;

import com.eventpro.TestcontainersConfiguration;
import com.eventpro.domain.enums.EventStatus;
import com.eventpro.dto.request.tickets.AttachTicketCategoryRequest;
import com.eventpro.entity.Event;
import com.eventpro.entity.users.Client;
import com.eventpro.entity.users.Organizer;
import com.eventpro.exception.conflict.DuplicateTicketCategoryException;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class EventTicketCategoryServiceIT {
    @Autowired EventTicketCategoryService eventTicketCategoryService;
    @Autowired EventTicketCategoryRepository eventTicketCategoryRepository;
    @Autowired EventRepository eventRepository;
    @Autowired OrganizerRepository organizerRepository;
    @Autowired TicketCategoryRepository ticketCategoryRepository;
    @Autowired ReservationLineRepository reservationLineRepository;
    @Autowired ReservationRepository reservationRepository;
    @Autowired PaymentRepository paymentRepository;
    @Autowired RefundRepository refundRepository;
    @Autowired TicketRepository ticketRepository;
    @Autowired PromotionRepository promotionRepository;
    @Autowired ClientRepository clientRepository;
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
    }

    @Test
    void concurrentAttachOfSameCategoryIsAcceptedOnce() throws Exception {
        String suffix = "attach-" + UUID.randomUUID();

        Organizer organizer = this.organizerRepository.save(Organizer.builder()
                .name("Organizer attach")
                .email("organizer-" + suffix + "@it.eventpro.dev")
                .organisationName("Organisation attach")
                .build());

        Instant now = this.clock.instant();
        Event event = this.eventRepository.save(Event.builder()
                .title("Event attach")
                .location("Hall attach")
                .startDate(now.plus(30, ChronoUnit.DAYS))
                .salesOpeningDate(now.minusSeconds(3_600))
                .salesClosingDate(now.plusSeconds(3_600))
                .capacity(600)
                .organizer(organizer)
                .build());

        AttachTicketCategoryRequest attachRequest = new AttachTicketCategoryRequest();
        attachRequest.setName("Gala Seats");
        attachRequest.setUnitPrice(new BigDecimal("50.00"));
        attachRequest.setTotalQuantity(100);
        attachRequest.setMaxQuotaPerClient(10);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Future<EventTicketCategoryServiceIT.AttachOutcome>> pendingOutcomes = new ArrayList<>();
        for (int workerNumber = 0; workerNumber < 2; workerNumber++) {
            pendingOutcomes.add(executor.submit((Callable<EventTicketCategoryServiceIT.AttachOutcome>) () -> {
                try {
                    return new EventTicketCategoryServiceIT.AttachOutcome(
                            this.eventTicketCategoryService.attach(organizer.getId(), event.getId(), attachRequest), null);
                } catch (RuntimeException attachFailure) {
                    return new EventTicketCategoryServiceIT.AttachOutcome(null, attachFailure);
                }
            }));
        }
        executor.shutdown();
        assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));

        List<EventTicketCategoryServiceIT.AttachOutcome> settledOutcomes = new ArrayList<>();
        for (Future<EventTicketCategoryServiceIT.AttachOutcome> pendingOutcome : pendingOutcomes) {
            settledOutcomes.add(pendingOutcome.get());
        }

        long acceptedAttachCalls = settledOutcomes.stream().filter(outcome -> outcome.attachment() != null).count();
        long refusedAttachCalls = settledOutcomes.stream().filter(outcome -> outcome.failure() != null).count();

        assertEquals(1, acceptedAttachCalls);
        assertEquals(1, refusedAttachCalls);
        for (EventTicketCategoryServiceIT.AttachOutcome settledOutcome : settledOutcomes) {
            if (settledOutcome.failure() != null) {
                assertInstanceOf(DuplicateTicketCategoryException.class, settledOutcome.failure());
            }
        }

        List<com.eventpro.entity.tickets.EventTicketCategory> storedAttachments =
                this.eventTicketCategoryRepository.findByEventId(event.getId());
        assertEquals(1, storedAttachments.size());
    }

    private record AttachOutcome(EventTicketCategoryService.CategoryStock attachment, RuntimeException failure) {}
}
